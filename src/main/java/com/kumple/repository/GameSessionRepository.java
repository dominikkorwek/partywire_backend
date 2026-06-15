package com.kumple.repository;

import com.kumple.model.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    Optional<GameSession> findByRoomCodeIgnoreCase(String code);
}
