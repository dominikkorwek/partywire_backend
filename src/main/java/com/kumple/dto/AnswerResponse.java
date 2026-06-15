package com.kumple.dto;

import com.kumple.model.Answer;
import com.kumple.model.enums.RoundStatus;

public record AnswerResponse(
        Long id,
        String content,
        PlayerResponse author,
        PlayerResponse targetPlayer,
        boolean correct,
        int voteCount
) {
    public static AnswerResponse from(Answer answer) {
        return from(answer, RoundStatus.COMPLETED);
    }

    public static AnswerResponse from(Answer answer, RoundStatus roundStatus) {
        boolean revealCorrect = roundStatus == RoundStatus.COMPLETED;
        return new AnswerResponse(
                answer.getId(),
                answer.getContent(),
                answer.getAuthor() != null ? PlayerResponse.from(answer.getAuthor()) : null,
                answer.getTargetPlayer() != null ? PlayerResponse.from(answer.getTargetPlayer()) : null,
                revealCorrect && answer.isCorrect(),
                answer.getVoteCount()
        );
    }
}
