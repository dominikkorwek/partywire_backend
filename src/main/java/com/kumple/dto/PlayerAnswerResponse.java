package com.kumple.dto;

public record PlayerAnswerResponse(
        PlayerResponse player,
        String answerText,
        boolean correct,
        boolean missed
) {}
