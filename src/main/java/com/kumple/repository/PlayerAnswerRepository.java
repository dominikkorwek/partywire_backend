package com.kumple.repository;

import com.kumple.model.PlayerAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerAnswerRepository extends JpaRepository<PlayerAnswer, Long> {

    List<PlayerAnswer> findByRoundId(Long roundId);

    Optional<PlayerAnswer> findByRoundIdAndPlayerPlayerId(Long roundId, String playerId);
}
