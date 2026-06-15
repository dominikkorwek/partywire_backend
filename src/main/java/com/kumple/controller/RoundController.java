package com.kumple.controller;

import com.kumple.dto.SubmitAnswerRequest;
import com.kumple.dto.SubmitQuestionRequest;
import com.kumple.service.GameService;
import com.kumple.service.RoundService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rounds")
public class RoundController {

    private final RoundService roundService;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public RoundController(RoundService roundService, GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.roundService = roundService;
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/{roundId}")
    public ResponseEntity<?> getRound(@PathVariable Long roundId) {
        try {
            return ResponseEntity.ok(roundService.getRoundState(roundId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{roundId}/classic-setup")
    public ResponseEntity<?> getClassicSetup(@PathVariable Long roundId, @RequestParam String playerId) {
        try {
            return ResponseEntity.ok(roundService.getClassicSetup(roundId, playerId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{roundId}/submit-question")
    public ResponseEntity<?> submitQuestion(@PathVariable Long roundId, @RequestBody SubmitQuestionRequest request) {
        try {
            roundService.submitQuestion(roundId, request);
            String roomCode = roundService.getRoomCode(roundId);
            var state = gameService.getState(roomCode);
            messagingTemplate.convertAndSend("/topic/room/" + roomCode.toUpperCase(), state);
            return ResponseEntity.ok(state);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{roundId}/submit-answer")
    public ResponseEntity<?> submitAnswer(@PathVariable Long roundId, @RequestBody SubmitAnswerRequest request) {
        try {
            roundService.submitAnswer(roundId, request);
            String roomCode = roundService.getRoomCode(roundId);
            var state = gameService.getState(roomCode);
            messagingTemplate.convertAndSend("/topic/room/" + roomCode.toUpperCase(), state);
            return ResponseEntity.ok(state);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{roundId}/expire-time")
    public ResponseEntity<?> expireTime(@PathVariable Long roundId) {
        try {
            roundService.expireRoundIfTimedOut(roundId);
            String roomCode = roundService.getRoomCode(roundId);
            var state = gameService.getState(roomCode);
            messagingTemplate.convertAndSend("/topic/room/" + roomCode.toUpperCase(), state);
            return ResponseEntity.ok(state);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
