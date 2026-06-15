package com.kumple.service;

import com.kumple.dto.ScoreResponse;
import com.kumple.model.GameSession;
import com.kumple.model.Player;
import com.kumple.model.Score;
import com.kumple.repository.ScoreRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScoreService {

    public static final int POINTS_PER_CORRECT_ANSWER = 10;

    private final ScoreRepository scoreRepository;

    public ScoreService(ScoreRepository scoreRepository) {
        this.scoreRepository = scoreRepository;
    }

    public void initializeScores(GameSession session) {
        session.getRoom().getPlayers().forEach(player ->
                scoreRepository.save(new Score(session, player))
        );
    }

    public void addPoint(GameSession session, Player player) {
        Score score = scoreRepository.findByGameSessionIdAndPlayerPlayerId(session.getId(), player.getPlayerId())
                .orElseGet(() -> scoreRepository.save(new Score(session, player)));
        score.addPoints(POINTS_PER_CORRECT_ANSWER);
        scoreRepository.save(score);
    }

    public List<ScoreResponse> getRanking(GameSession session) {
        return scoreRepository.findByGameSessionIdOrderByPointsDesc(session.getId())
                .stream()
                .map(ScoreResponse::from)
                .toList();
    }

    public boolean hasReachedPointLimit(GameSession session) {
        return scoreRepository.findByGameSessionIdOrderByPointsDesc(session.getId())
                .stream()
                .anyMatch(score -> score.getPoints() >= session.getPointLimit());
    }
}
