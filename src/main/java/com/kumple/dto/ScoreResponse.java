package com.kumple.dto;

import com.kumple.model.Score;

public record ScoreResponse(
        PlayerResponse player,
        int points
) {
    public static ScoreResponse from(Score score) {
        return new ScoreResponse(PlayerResponse.from(score.getPlayer()), score.getPoints());
    }
}
