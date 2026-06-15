package com.kumple.dto;

import com.kumple.model.GameSession;
import com.kumple.model.QuestionCategory;
import com.kumple.model.enums.GameStatus;
import com.kumple.model.enums.RoundType;

import java.util.List;

public record GameStateResponse(
        Long id,
        GameStatus status,
        RoomResponse room,
        int pointLimit,
        int timePerAnswer,
        RoundResponse currentRound,
        List<ScoreResponse> ranking,
        List<Long> excludedCategoryIds,
        List<RoundType> excludedRoundTypes
) {
    public static GameStateResponse from(GameSession session, List<ScoreResponse> ranking) {
        return new GameStateResponse(
                session.getId(),
                session.getStatus(),
                RoomResponse.from(session.getRoom()),
                session.getPointLimit(),
                session.getTimePerAnswer(),
                RoundResponse.from(session.getCurrentRound()),
                ranking,
                session.getExcludedCategories().stream().map(QuestionCategory::getId).toList(),
                List.copyOf(session.getExcludedRoundTypes())
        );
    }
}
