package com.kumple.repository;

import com.kumple.model.Score;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScoreRepository extends JpaRepository<Score, Long> {

    List<Score> findByGameSessionIdOrderByPointsDesc(Long gameSessionId);

    Optional<Score> findByGameSessionIdAndPlayerPlayerId(Long gameSessionId, String playerId);
}
