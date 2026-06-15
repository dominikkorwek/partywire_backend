package com.kumple.repository;

import com.kumple.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByCodeIgnoreCase(String code);

    void deleteByCodeIgnoreCase(String code);

    Optional<Room> findByHostAuthSubject(String hostAuthSubject);
}
