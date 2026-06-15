package com.kumple.service;

import com.kumple.dto.ClassicOptionResponse;
import com.kumple.dto.ClassicSetupResponse;
import com.kumple.dto.AnswerOptionRequest;
import com.kumple.dto.PlayerAnswerResponse;
import com.kumple.dto.PlayerResponse;
import com.kumple.dto.RoundResponse;
import com.kumple.dto.SubmitAnswerRequest;
import com.kumple.dto.SubmitQuestionRequest;
import com.kumple.model.*;
import com.kumple.model.enums.GameStatus;
import com.kumple.model.enums.RoundStatus;
import com.kumple.model.enums.RoundType;
import com.kumple.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoundService {

    private final RoundRepository roundRepository;
    private final AnswerRepository answerRepository;
    private final PlayerAnswerRepository playerAnswerRepository;
    private final GameSessionRepository gameSessionRepository;
    private final QuestionService questionService;
    private final ScoreService scoreService;

    public RoundService(
            RoundRepository roundRepository,
            AnswerRepository answerRepository,
            PlayerAnswerRepository playerAnswerRepository,
            GameSessionRepository gameSessionRepository,
            QuestionService questionService,
            ScoreService scoreService
    ) {
        this.roundRepository = roundRepository;
        this.answerRepository = answerRepository;
        this.playerAnswerRepository = playerAnswerRepository;
        this.gameSessionRepository = gameSessionRepository;
        this.questionService = questionService;
        this.scoreService = scoreService;
    }

    @Transactional
    public Round createNextRound(GameSession session) {
        int roundNumber = session.getCurrentRoundNumber() + 1;
        RoundType roundType = chooseRoundType(session, roundNumber);
        Player selectedPlayer = roundType == RoundType.VOTE_PERSON ? null : pickPlayer(session, roundNumber);
        Question question = null;

        if (roundType == RoundType.GUESS_PLAYER_ANSWER) {
            question = questionService.getRandomQuestion(session, RoundType.GUESS_PLAYER_ANSWER);
        } else if (roundType == RoundType.REUSE_QUESTION) {
            question = questionService.getRandomQuestion(session, RoundType.REUSE_QUESTION);
        } else if (roundType == RoundType.VOTE_PERSON || roundType == RoundType.BEST_ANSWER) {
            question = questionService.getRandomQuestion(session, roundType);
        }

        RoundStatus initialStatus = statusAfterQuestionSetup(roundType);
        Round round = roundRepository.save(new Round(session, roundType, roundNumber, selectedPlayer, question, initialStatus));

        if (roundType == RoundType.REUSE_QUESTION) {
            createClassicAnswers(round, question);
            round = roundRepository.save(round);
        } else if (roundType == RoundType.VOTE_PERSON) {
            createPlayerAnswers(round);
            round = roundRepository.save(round);
        }

        if (round.getStatus() == RoundStatus.WAITING_FOR_ANSWERS) {
            beginAnswerPhase(round);
            round = roundRepository.save(round);
        }

        session.setCurrentRound(round);
        session.setCurrentRoundNumber(roundNumber);
        gameSessionRepository.save(session);
        return round;
    }

    @Transactional
    public boolean expireRoundIfTimedOut(Long roundId) {
        Round round = getRound(roundId);
        if (round.getAnswerPhaseStartedAt() == null) {
            return false;
        }
        if (round.getStatus() != RoundStatus.WAITING_FOR_ANSWERS && round.getStatus() != RoundStatus.REVEALING) {
            return false;
        }

        Instant deadline = round.getAnswerPhaseStartedAt()
                .plusSeconds(round.getGameSession().getTimePerAnswer());
        if (Instant.now().isBefore(deadline)) {
            return false;
        }

        forceCompleteDueToTimeout(round);
        roundRepository.save(round);
        gameSessionRepository.save(round.getGameSession());
        return true;
    }

    @Transactional
    public RoundResponse submitQuestion(Long roundId, SubmitQuestionRequest request) {
        Round round = getRound(roundId);
        if (round.getStatus() != RoundStatus.WAITING_FOR_QUESTION) {
            throw new IllegalStateException("Ta runda nie oczekuje teraz na pytanie lub warianty odpowiedzi");
        }

        Player player = requireSelectedPlayer(round, request.playerId(), "Tylko wskazany gracz może przygotować tę rundę");

        if (round.getRoundType() == RoundType.REUSE_QUESTION) {
            return submitClassicCorrectAnswer(round, player, request);
        }

        if (hasText(request.questionContent())) {
            Question question = questionService.createSessionQuestion(
                    round.getGameSession(),
                    request.questionContent().trim(),
                    round.getRoundType()
            );
            round.setQuestion(question);
        }

        if (Boolean.TRUE.equals(request.answersArePlayers())) {
            createPlayerAnswers(round);
        } else if (request.answers() != null) {
            for (AnswerOptionRequest option : request.answers()) {
                if (hasText(option.content())) {
                    Answer answer = new Answer(round, option.content().trim(), null, null, Boolean.TRUE.equals(option.correct()));
                    round.getAnswers().add(answerRepository.save(answer));
                }
            }
        }

        if (round.getQuestion() == null) {
            throw new IllegalArgumentException("Runda musi mieć pytanie");
        }
        if (round.getAnswers().isEmpty()) {
            throw new IllegalArgumentException("Runda musi mieć przynajmniej jedną odpowiedź");
        }

        round.setStatus(RoundStatus.WAITING_FOR_ANSWERS);
        beginAnswerPhase(round);
        return toRoundResponse(roundRepository.save(round));
    }

    @Transactional
    public RoundResponse submitAnswer(Long roundId, SubmitAnswerRequest request) {
        Round round = getRound(roundId);
        Player player = requireActivePlayer(round, request.playerId());

        if (round.getStatus() == RoundStatus.REVEALING
                && round.getRoundType() == RoundType.BEST_ANSWER
                && request.answerId() != null) {
            return chooseBestAnswer(round, player, request.answerId());
        }

        if (request.selectedAnswerId() != null) {
            return chooseBestAnswer(round, player, request.selectedAnswerId());
        }

        if (round.getStatus() != RoundStatus.WAITING_FOR_ANSWERS) {
            throw new IllegalStateException("Ta runda nie przyjmuje teraz odpowiedzi");
        }
        playerAnswerRepository.findByRoundIdAndPlayerPlayerId(roundId, request.playerId()).ifPresent(existing -> {
            throw new IllegalArgumentException("Ten gracz już odpowiedział w tej rundzie");
        });

        Answer answer = null;
        String freeText = null;
        if (hasText(request.freeText())) {
            freeText = request.freeText().trim();
            answer = answerRepository.save(new Answer(round, freeText, player, null, false));
            round.getAnswers().add(answer);
        } else if (request.answerId() != null) {
            answer = answerRepository.findById(request.answerId())
                    .orElseThrow(() -> new IllegalArgumentException("Odpowiedź nie istnieje"));
            if (!Objects.equals(answer.getRound().getId(), round.getId())) {
                throw new IllegalArgumentException("Odpowiedź nie należy do tej rundy");
            }
            answer.incrementVoteCount();
            answerRepository.save(answer);
        } else {
            throw new IllegalArgumentException("Brak odpowiedzi");
        }

        PlayerAnswer playerAnswer = new PlayerAnswer(round, player, answer, freeText);
        round.getPlayerAnswers().add(playerAnswerRepository.save(playerAnswer));

        if (round.getRoundType() == RoundType.BEST_ANSWER && hasAllExpectedAnswers(round)) {
            round.setStatus(RoundStatus.REVEALING);
            beginAnswerPhase(round);
        } else if (round.getRoundType() != RoundType.BEST_ANSWER && hasAllExpectedAnswers(round)) {
            completeRound(round);
        }

        return toRoundResponse(roundRepository.save(round));
    }

    @Transactional(readOnly = true)
    public RoundResponse getRoundState(Long roundId) {
        return toRoundResponse(getRound(roundId));
    }

    public RoundResponse toRoundResponse(Round round) {
        if (round == null) return null;
        List<PlayerAnswerResponse> summaries = round.getStatus() == RoundStatus.COMPLETED
                ? buildPlayerAnswerSummaries(round)
                : List.of();
        return RoundResponse.from(round, summaries);
    }

    @Transactional(readOnly = true)
    public ClassicSetupResponse getClassicSetup(Long roundId, String playerId) {
        Round round = getRound(roundId);
        if (round.getRoundType() != RoundType.REUSE_QUESTION) {
            throw new IllegalStateException("To nie jest runda klasycznego pytania");
        }
        if (round.getStatus() != RoundStatus.WAITING_FOR_QUESTION) {
            throw new IllegalStateException("Ta runda nie oczekuje teraz na wybór poprawnej odpowiedzi");
        }
        requireSelectedPlayer(round, playerId, "Tylko wskazany gracz może przygotować to pytanie");
        List<ClassicOptionResponse> options = round.getAnswers().stream()
                .map(answer -> new ClassicOptionResponse(answer.getId(), answer.getContent()))
                .toList();
        if (options.size() < 4) {
            throw new IllegalStateException("Brak opcji odpowiedzi dla tego pytania");
        }
        String questionContent = round.getQuestion() != null ? round.getQuestion().getContent() : "";
        return new ClassicSetupResponse(questionContent, options);
    }

    @Transactional(readOnly = true)
    public String getRoomCode(Long roundId) {
        return getRound(roundId).getGameSession().getRoom().getCode();
    }

    private RoundResponse chooseBestAnswer(Round round, Player player, Long selectedAnswerId) {
        if (round.getRoundType() != RoundType.BEST_ANSWER) {
            throw new IllegalStateException("Wybor najlepszej odpowiedzi dotyczy tylko rundy BEST_ANSWER");
        }
        if (round.getSelectedPlayer() == null || !round.getSelectedPlayer().getPlayerId().equals(player.getPlayerId())) {
            throw new IllegalArgumentException("Tylko wskazany gracz może wybrać najlepszą odpowiedź");
        }
        if (round.getStatus() != RoundStatus.REVEALING) {
            throw new IllegalStateException("Najpierw pozostali gracze muszą przesłać swoje odpowiedzi");
        }

        Answer winner = answerRepository.findById(selectedAnswerId)
                .orElseThrow(() -> new IllegalArgumentException("Odpowiedź nie istnieje"));
        if (!Objects.equals(winner.getRound().getId(), round.getId())) {
            throw new IllegalArgumentException("Odpowiedź nie należy do tej rundy");
        }
        if (winner.getAuthor() == null) {
            throw new IllegalArgumentException("Wybierz odpowiedź od innego gracza");
        }

        winner.setCorrect(true);
        round.setWinningAnswer(winner);
        if (winner.getAuthor() != null) {
            playerAnswerRepository.findByRoundIdAndPlayerPlayerId(round.getId(), winner.getAuthor().getPlayerId())
                    .ifPresent(playerAnswer -> {
                        playerAnswer.setAwardedPoint(true);
                        playerAnswerRepository.save(playerAnswer);
                    });
            scoreService.addPoint(round.getGameSession(), winner.getAuthor());
        }
        finishRound(round);
        return toRoundResponse(roundRepository.save(round));
    }

    private void completeRound(Round round) {
        if (round.getRoundType() == RoundType.GUESS_PLAYER_ANSWER || round.getRoundType() == RoundType.REUSE_QUESTION) {
            List<Answer> winners = round.getAnswers().stream().filter(Answer::isCorrect).toList();
            winners.stream().findFirst().ifPresent(round::setWinningAnswer);
            awardPlayersWhoSelected(round, winners);
        } else if (round.getRoundType() == RoundType.VOTE_PERSON) {
            completeVotePersonRound(round);
            return;
        } else if (round.getRoundType() == RoundType.BEST_ANSWER) {
            if (round.getWinningAnswer() == null) {
                int maxVotes = round.getAnswers().stream().mapToInt(Answer::getVoteCount).max().orElse(0);
                List<Answer> winners = round.getAnswers().stream()
                        .filter(answer -> answer.getVoteCount() == maxVotes && maxVotes > 0)
                        .toList();
                winners.stream().findFirst().ifPresent(round::setWinningAnswer);
                awardPlayersWhoSelected(round, winners);
            }
        } else {
            int maxVotes = round.getAnswers().stream().mapToInt(Answer::getVoteCount).max().orElse(0);
            List<Answer> winners = round.getAnswers().stream()
                    .filter(answer -> answer.getVoteCount() == maxVotes && maxVotes > 0)
                    .toList();
            winners.stream().findFirst().ifPresent(round::setWinningAnswer);
            awardPlayersWhoSelected(round, winners);
        }
        finishRound(round);
    }

    private void completeVotePersonRound(Round round) {
        int maxVotes = round.getAnswers().stream().mapToInt(Answer::getVoteCount).max().orElse(0);
        List<Answer> topAnswers = round.getAnswers().stream()
                .filter(answer -> answer.getVoteCount() == maxVotes && maxVotes > 0)
                .toList();

        if (topAnswers.size() == 1) {
            round.setWinningAnswer(topAnswers.get(0));
            awardPlayersWhoSelected(round, topAnswers);
            finishRound(round);
            return;
        }

        if (topAnswers.isEmpty()) {
            finishRound(round);
            return;
        }

        if (round.isTiebreakRevote()) {
            finishRound(round);
            return;
        }

        startVotePersonTiebreak(round, topAnswers);
    }

    private void startVotePersonTiebreak(Round round, List<Answer> tiedAnswers) {
        List<PlayerAnswer> existingAnswers = playerAnswerRepository.findByRoundId(round.getId());
        playerAnswerRepository.deleteAll(existingAnswers);
        round.getPlayerAnswers().clear();

        Set<Long> tiedPlayerIds = new HashSet<>();
        for (Answer tiedAnswer : tiedAnswers) {
            if (tiedAnswer.getTargetPlayer() != null) {
                tiedPlayerIds.add(tiedAnswer.getTargetPlayer().getId());
            }
        }

        List<Answer> toRemove = round.getAnswers().stream()
                .filter(answer -> answer.getTargetPlayer() == null || !tiedPlayerIds.contains(answer.getTargetPlayer().getId()))
                .toList();
        for (Answer answer : toRemove) {
            round.getAnswers().remove(answer);
            answerRepository.delete(answer);
        }

        for (Answer answer : round.getAnswers()) {
            answer.setVoteCount(0);
            answerRepository.save(answer);
        }

        round.setWinningAnswer(null);
        round.setTiebreakRevote(true);
        round.setStatus(RoundStatus.WAITING_FOR_ANSWERS);
        beginAnswerPhase(round);
    }

    private void finishRound(Round round) {
        round.setStatus(RoundStatus.COMPLETED);
        round.setCompletedAt(Instant.now());
        if (scoreService.hasReachedPointLimit(round.getGameSession())) {
            round.getGameSession().setStatus(GameStatus.FINISHED);
        }
        gameSessionRepository.save(round.getGameSession());
    }

    private void awardPlayersWhoSelected(Round round, List<Answer> winners) {
        List<Long> winnerIds = winners.stream().map(Answer::getId).toList();
        for (PlayerAnswer playerAnswer : playerAnswerRepository.findByRoundId(round.getId())) {
            if (playerAnswer.isAwardedPoint()) {
                continue;
            }
            boolean matched = false;
            if (playerAnswer.getAnswer() != null) {
                matched = winnerIds.contains(playerAnswer.getAnswer().getId());
            }
            if (matched) {
                playerAnswer.setAwardedPoint(true);
                playerAnswerRepository.save(playerAnswer);
                scoreService.addPoint(round.getGameSession(), playerAnswer.getPlayer());
            }
        }
    }

    private void beginAnswerPhase(Round round) {
        round.setAnswerPhaseStartedAt(Instant.now());
    }

    private void forceCompleteDueToTimeout(Round round) {
        if (round.getStatus() == RoundStatus.WAITING_FOR_ANSWERS) {
            if (round.getRoundType() == RoundType.BEST_ANSWER) {
                long submitted = playerAnswerRepository.findByRoundId(round.getId()).size();
                if (submitted > 0) {
                    round.setStatus(RoundStatus.REVEALING);
                    beginAnswerPhase(round);
                    return;
                }
            }
            completeRound(round);
            return;
        }

        if (round.getStatus() == RoundStatus.REVEALING && round.getRoundType() == RoundType.BEST_ANSWER) {
            completeRound(round);
        }
    }

    private List<PlayerAnswerResponse> buildPlayerAnswerSummaries(Round round) {
        Map<String, PlayerAnswer> byPlayerId = playerAnswerRepository.findByRoundId(round.getId()).stream()
                .collect(Collectors.toMap(pa -> pa.getPlayer().getPlayerId(), pa -> pa));

        return round.getGameSession().getRoom().getPlayers().stream()
                .filter(player -> shouldShowInSummary(round, player, byPlayerId))
                .sorted(Comparator.comparing(Player::getNickname, String.CASE_INSENSITIVE_ORDER))
                .map(player -> {
                    PlayerAnswer playerAnswer = byPlayerId.get(player.getPlayerId());
                    if (playerAnswer == null) {
                        return new PlayerAnswerResponse(
                                PlayerResponse.from(player),
                                null,
                                false,
                                true
                        );
                    }
                    return new PlayerAnswerResponse(
                            PlayerResponse.from(player),
                            resolveAnswerText(playerAnswer, round),
                            playerAnswer.isAwardedPoint(),
                            false
                    );
                })
                .toList();
    }

    private boolean shouldShowInSummary(Round round, Player player, Map<String, PlayerAnswer> answersByPlayerId) {
        if (round.getRoundType() == RoundType.BEST_ANSWER) {
            return answersByPlayerId.containsKey(player.getPlayerId()) || shouldAnswer(round, player);
        }
        return shouldAnswer(round, player);
    }

    private String resolveAnswerText(PlayerAnswer playerAnswer, Round round) {
        if (round.getRoundType() == RoundType.BEST_ANSWER) {
            if (playerAnswer.getFreeText() != null && !playerAnswer.getFreeText().isBlank()) {
                return playerAnswer.getFreeText().trim();
            }
            if (playerAnswer.getAnswer() != null) {
                return playerAnswer.getAnswer().getContent();
            }
        }
        if (playerAnswer.getAnswer() != null) {
            return playerAnswer.getAnswer().getContent();
        }
        if (playerAnswer.getFreeText() != null && !playerAnswer.getFreeText().isBlank()) {
            return playerAnswer.getFreeText().trim();
        }
        return null;
    }

    private RoundStatus statusAfterQuestionSetup(RoundType roundType) {
        return switch (roundType) {
            case GUESS_PLAYER_ANSWER, REUSE_QUESTION, PLAYER_CREATES_QUESTION -> RoundStatus.WAITING_FOR_QUESTION;
            case VOTE_PERSON, BEST_ANSWER -> RoundStatus.WAITING_FOR_ANSWERS;
        };
    }

    private boolean hasAllExpectedAnswers(Round round) {
        long expected = round.getGameSession().getRoom().getPlayers().stream()
                .filter(player -> shouldAnswer(round, player))
                .count();
        long actual = playerAnswerRepository.findByRoundId(round.getId()).size();
        return expected > 0 && actual >= expected;
    }

    private boolean shouldAnswer(Round round, Player player) {
        if (round.getRoundType() == RoundType.VOTE_PERSON) return true;
        return round.getSelectedPlayer() == null || !round.getSelectedPlayer().getPlayerId().equals(player.getPlayerId());
    }

    private void createPlayerAnswers(Round round) {
        for (Player player : round.getGameSession().getRoom().getPlayers()) {
            Answer answer = new Answer(round, player.getNickname(), null, player, false);
            round.getAnswers().add(answerRepository.save(answer));
        }
    }

    private void createClassicAnswers(Round round, Question question) {
        for (QuestionOption option : questionService.getOptions(question)) {
            Answer answer = new Answer(round, option.getContent(), null, null, false);
            round.getAnswers().add(answerRepository.save(answer));
        }
    }

    private RoundResponse submitClassicCorrectAnswer(Round round, Player player, SubmitQuestionRequest request) {
        if (request.correctAnswerId() == null) {
            throw new IllegalArgumentException("Wybierz poprawną odpowiedź");
        }

        Answer correct = answerRepository.findById(request.correctAnswerId())
                .orElseThrow(() -> new IllegalArgumentException("Odpowiedź nie istnieje"));
        if (!Objects.equals(correct.getRound().getId(), round.getId())) {
            throw new IllegalArgumentException("Odpowiedź nie należy do tej rundy");
        }

        correct.setCorrect(true);
        answerRepository.save(correct);
        round.setStatus(RoundStatus.WAITING_FOR_ANSWERS);
        beginAnswerPhase(round);
        return toRoundResponse(roundRepository.save(round));
    }

    private Player requireActivePlayer(Round round, String playerId) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("Brak identyfikatora gracza");
        }
        Player player = round.getGameSession().getRoom().findByPlayerId(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Gracz nie należy już do tego pokoju");
        }
        return player;
    }

    private Player requireSelectedPlayer(Round round, String playerId, String errorMessage) {
        Player player = requireActivePlayer(round, playerId);
        if (round.getSelectedPlayer() == null || !round.getSelectedPlayer().getPlayerId().equals(player.getPlayerId())) {
            throw new IllegalArgumentException(errorMessage);
        }
        return player;
    }

    private RoundType chooseRoundType(GameSession session, int roundNumber) {
        List<RoundType> allowed = Arrays.stream(RoundType.values())
                .filter(type -> !session.getExcludedRoundTypes().contains(type))
                .toList();
        if (allowed.isEmpty()) {
            throw new IllegalStateException("Brak dostępnych typów rund");
        }
        return allowed.get((roundNumber - 1) % allowed.size());
    }

    private Player pickPlayer(GameSession session, int roundNumber) {
        List<Player> players = session.getRoom().getPlayers().stream()
                .sorted(Comparator.comparing(Player::getJoinedAt))
                .toList();
        if (players.isEmpty()) {
            throw new IllegalStateException("Nie można rozpocząć rundy bez graczy");
        }
        return players.get((roundNumber - 1) % players.size());
    }

    private Round getRound(Long roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Runda nie istnieje"));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
