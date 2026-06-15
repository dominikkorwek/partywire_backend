package com.kumple.repository;

import com.kumple.model.GameSession;
import com.kumple.model.Question;
import com.kumple.model.enums.RoundType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByPredefinedTrueAndRoundTypeAndCategoryIdNotIn(RoundType roundType, Collection<Long> excludedCategoryIds);

    List<Question> findByPredefinedTrueAndRoundType(RoundType roundType);

    List<Question> findByGameSessionAndRoundType(GameSession gameSession, RoundType roundType);
}
