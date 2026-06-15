package com.kumple.dto;

public record SubmitAnswerRequest(
        String playerId,
        Long answerId,
        String freeText,
        Long selectedAnswerId
) {}
