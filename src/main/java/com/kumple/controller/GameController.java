package com.kumple.controller;

import com.kumple.dto.GameSettingsRequest;
import com.kumple.dto.GameStateResponse;
import com.kumple.dto.QuestionCategoryResponse;
import com.kumple.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GameController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameController(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/categories")
    public List<QuestionCategoryResponse> getCategories() {
        return gameService.getCategories().stream().map(QuestionCategoryResponse::from).toList();
    }

    @GetMapping("/rooms/{code}/game")
    public ResponseEntity<?> getState(@PathVariable String code) {
        try {
            return ResponseEntity.ok(gameService.getState(code));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/rooms/{code}/settings")
    public ResponseEntity<?> updateSettings(
            @PathVariable String code,
            @RequestBody GameSettingsRequest request,
            Authentication authentication
    ) {
        try {
            String subject = authentication != null ? authentication.getName() : null;
            GameStateResponse state = gameService.updateSettings(code, request, subject);
            broadcast(code, state);
            return ResponseEntity.ok(state);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/rooms/{code}/start")
    public ResponseEntity<?> startGame(@PathVariable String code, Authentication authentication) {
        try {
            String subject = authentication != null ? authentication.getName() : null;
            GameStateResponse state = gameService.startGame(code, subject);
            broadcast(code, state);
            return ResponseEntity.ok(state);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/rooms/{code}/rounds/next")
    public ResponseEntity<?> nextRound(@PathVariable String code, Authentication authentication) {
        try {
            String subject = authentication != null ? authentication.getName() : null;
            GameStateResponse state = gameService.nextRound(code, subject);
            broadcast(code, state);
            return ResponseEntity.ok(state);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/rooms/{code}/reset-lobby")
    public ResponseEntity<?> resetToLobby(@PathVariable String code, Authentication authentication) {
        try {
            String subject = authentication != null ? authentication.getName() : null;
            GameStateResponse state = gameService.resetToLobby(code, subject);
            broadcast(code, state);
            return ResponseEntity.ok(state);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    private void broadcast(String code, GameStateResponse state) {
        messagingTemplate.convertAndSend("/topic/room/" + code.toUpperCase(), state);
    }
}
