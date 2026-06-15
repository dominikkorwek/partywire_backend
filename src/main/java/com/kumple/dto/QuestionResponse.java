package com.kumple.dto;

import com.kumple.model.Question;
import com.kumple.model.enums.RoundType;

public record QuestionResponse(
        Long id,
        String content,
        RoundType roundType,
        String category
) {
    public static QuestionResponse from(Question question) {
        if (question == null) return null;
        return new QuestionResponse(
                question.getId(),
                question.getContent(),
                question.getRoundType(),
                question.getCategory() != null ? question.getCategory().getName() : null
        );
    }
}
