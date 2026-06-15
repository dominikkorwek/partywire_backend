package com.kumple.config;

import com.kumple.dto.GameStateResponse;
import com.kumple.dto.RoomResponse;
import com.kumple.service.GameService;
import com.kumple.service.RoomService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RoomPresenceMonitor {
    private final RoomService roomService;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;
    private final long presenceTimeoutMs;

    public RoomPresenceMonitor(
            RoomService roomService,
            GameService gameService,
            SimpMessagingTemplate messagingTemplate,
            @Value("${app.presence-timeout-ms}") long presenceTimeoutMs
    ) {
        this.roomService = roomService;
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
        this.presenceTimeoutMs = presenceTimeoutMs;
    }

    @Scheduled(fixedDelayString = "${app.presence-check-interval-ms}")
    public void evictInactivePlayers() {
        for (RoomService.DisconnectResult result : roomService.evictInactiveSessions(presenceTimeoutMs)) {
            String destination = "/topic/room/" + result.roomCode().toUpperCase();
            if (result.roomClosed()) {
                messagingTemplate.convertAndSend(destination, Map.of("event", "ROOM_CLOSED"));
                continue;
            }

            GameStateResponse gameState = gameService.handlePlayerDisconnected(result.roomCode(), result.playerId());
            if (gameState != null) {
                messagingTemplate.convertAndSend(destination, gameState);
                continue;
            }

            if (result.room() != null) {
                messagingTemplate.convertAndSend(destination, RoomResponse.from(result.room()));
            }
        }
    }
}
