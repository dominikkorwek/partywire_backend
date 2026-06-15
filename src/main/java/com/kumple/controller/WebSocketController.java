package com.kumple.controller;

import com.kumple.service.RoomService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class WebSocketController {

    private final RoomService roomService;

    public WebSocketController(RoomService roomService) {
        this.roomService = roomService;
    }

    @MessageMapping("/register")
    public void registerSession(@Payload Map<String, String> payload,
                                SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String roomCode = payload.get("roomCode");
        String playerId = payload.get("playerId");

        if (sessionId != null && roomCode != null && playerId != null) {
            roomService.registerSession(sessionId, roomCode, playerId);
        }
    }
}
