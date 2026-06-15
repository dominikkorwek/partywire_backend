package com.kumple.dto;

import com.kumple.model.QuestionCategory;

public record QuestionCategoryResponse(
        Long id,
        String name
) {
    public static QuestionCategoryResponse from(QuestionCategory category) {
        return new QuestionCategoryResponse(category.getId(), category.getName());
    }
}
