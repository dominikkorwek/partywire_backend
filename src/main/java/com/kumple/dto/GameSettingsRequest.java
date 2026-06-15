package com.kumple.dto;

import com.kumple.model.enums.RoundType;

import java.util.Set;

public record GameSettingsRequest(
        Integer pointLimit,
        Integer timePerAnswer,
        Set<Long> excludedCategoryIds,
        Set<RoundType> excludedRoundTypes
) {}
