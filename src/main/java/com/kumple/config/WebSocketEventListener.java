package com.kumple.config;

import com.kumple.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final RoomService roomService;

    public WebSocketEventListener(RoomService roomService) {
        this.roomService = roomService;
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        roomService.getSession(sessionId).ifPresent(session -> {
            log.info("WebSocket disconnect: session={}, room={}, player={}",
                    sessionId, session.roomCode(), session.playerId());

            roomService.unregisterSession(sessionId);
        });
    }
}
