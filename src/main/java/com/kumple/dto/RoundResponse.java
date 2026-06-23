package com.kumple.dto;

import com.kumple.model.Round;
import com.kumple.model.enums.RoundStatus;
import com.kumple.model.enums.RoundType;

import java.util.Comparator;
import java.util.List;

public record RoundResponse(
        Long id,
        int roundNumber,
        RoundType roundType,
        RoundStatus status,
        QuestionResponse question,
        PlayerResponse selectedPlayer,
        AnswerResponse winningAnswer,
        List<AnswerResponse> answers,
        boolean tiebreakRevote,
        String answerPhaseStartedAt,
        List<PlayerAnswerResponse> playerAnswers
) {
    public static RoundResponse from(Round round) {
        return from(round, List.of());
    }

    public static RoundResponse from(Round round, List<PlayerAnswerResponse> playerAnswers) {
        if (round == null) return null;
        RoundStatus status = round.getStatus();
        boolean hideAnswers = status == RoundStatus.WAITING_FOR_QUESTION;
        return new RoundResponse(
                round.getId(),
                round.getRoundNumber(),
                round.getRoundType(),
                status,
                QuestionResponse.from(round.getQuestion()),
                round.getSelectedPlayer() != null ? PlayerResponse.from(round.getSelectedPlayer()) : null,
                round.getWinningAnswer() != null ? AnswerResponse.from(round.getWinningAnswer(), status) : null,
                hideAnswers
                        ? List.of()
                        : round.getAnswers().stream()
                                .sorted(Comparator.comparing(a -> a.getId() != null ? a.getId() : Long.MAX_VALUE))
                                .map(a -> AnswerResponse.from(a, status))
                                .toList(),
                round.isTiebreakRevote(),
                round.getAnswerPhaseStartedAt() != null ? round.getAnswerPhaseStartedAt().toString() : null,
                playerAnswers != null ? playerAnswers : List.of()
        );
    }
}
