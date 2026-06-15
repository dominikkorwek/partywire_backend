package com.kumple.repository;

import com.kumple.model.Round;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoundRepository extends JpaRepository<Round, Long> {

    Optional<Round> findTopByGameSessionRoomCodeIgnoreCaseOrderByRoundNumberDesc(String code);
}
