package com.kumple.dto;

public record AnswerOptionRequest(
        String content,
        Boolean correct,
        String targetPlayerId
) {}
