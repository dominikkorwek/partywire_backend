package com.kumple.controller;

import com.kumple.dto.*;
import com.kumple.model.Player;
import com.kumple.model.Room;
import com.kumple.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomController(RoomService roomService, SimpMessagingTemplate messagingTemplate) {
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@RequestBody CreateRoomRequest request, Authentication authentication) {
        String subject = authentication != null ? authentication.getName() : null;
        Room room = roomService.createRoom(request.hostNickname(), request.maxPlayers(), request.avatarAnimal(), request.avatarColor(), subject);
        return ResponseEntity.status(HttpStatus.CREATED).body(RoomResponse.from(room));
    }

    @GetMapping("/{code}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String code) {
        return roomService.getRoom(code)
                .map(room -> ResponseEntity.ok(RoomResponse.from(room)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<?> joinRoom(@PathVariable String code, @RequestBody JoinRoomRequest request) {
        try {
            Player player = roomService.joinRoom(code, request.nickname(), request.avatarAnimal(), request.avatarColor());
            Room room = roomService.getRoom(code).orElseThrow();

            RoomResponse roomResponse = RoomResponse.from(room);
            messagingTemplate.convertAndSend("/topic/room/" + code.toUpperCase(), roomResponse);

            return ResponseEntity.ok(Map.of(
                    "player", PlayerResponse.from(player),
                    "room", roomResponse
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{code}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable String code, @RequestBody LeaveRoomRequest request) {
        boolean roomDestroyed = roomService.leaveRoom(code, request.playerId());

        if (roomDestroyed) {
            messagingTemplate.convertAndSend(
                    "/topic/room/" + code.toUpperCase(),
                    Map.of("event", "ROOM_CLOSED")
            );
            return ResponseEntity.ok(Map.of("message", "Pokój zamknięty (host wyszedł)"));
        }

        return roomService.getRoom(code)
                .map(room -> {
                    RoomResponse roomResponse = RoomResponse.from(room);
                    messagingTemplate.convertAndSend("/topic/room/" + code.toUpperCase(), roomResponse);
                    return ResponseEntity.ok(Map.of("message", "Opuszczono pokój"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{code}/close")
    public ResponseEntity<?> closeRoom(@PathVariable String code, Authentication authentication) {
        try {
            String subject = authentication != null ? authentication.getName() : null;
            roomService.closeRoom(code, subject);
            messagingTemplate.convertAndSend(
                    "/topic/room/" + code.toUpperCase(),
                    Map.of("event", "ROOM_CLOSED")
            );
            return ResponseEntity.ok(Map.of("message", "Pokój zamknięty"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }
}
