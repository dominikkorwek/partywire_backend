package com.kumple.service;

import com.kumple.model.GameSession;
import com.kumple.model.Question;
import com.kumple.model.QuestionCategory;
import com.kumple.model.QuestionOption;
import com.kumple.model.enums.RoundType;
import com.kumple.repository.QuestionCategoryRepository;
import com.kumple.repository.QuestionOptionRepository;
import com.kumple.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionCategoryRepository categoryRepository;
    private final QuestionOptionRepository optionRepository;
    private final SecureRandom random = new SecureRandom();

    public QuestionService(
            QuestionRepository questionRepository,
            QuestionCategoryRepository categoryRepository,
            QuestionOptionRepository optionRepository
    ) {
        this.questionRepository = questionRepository;
        this.categoryRepository = categoryRepository;
        this.optionRepository = optionRepository;
    }

    public Question getRandomQuestion(GameSession session, RoundType roundType) {
        Set<Long> excludedCategoryIds = session.getExcludedCategories()
                .stream()
                .map(QuestionCategory::getId)
                .collect(java.util.stream.Collectors.toSet());

        List<Question> questions = excludedCategoryIds.isEmpty()
                ? questionRepository.findByPredefinedTrueAndRoundType(roundType)
                : questionRepository.findByPredefinedTrueAndRoundTypeAndCategoryIdNotIn(roundType, excludedCategoryIds);

        if (questions.isEmpty()) {
            questions = questionRepository.findByPredefinedTrueAndRoundType(roundType);
        }
        if (questions.isEmpty()) {
            throw new IllegalStateException("Brak pytań dla typu rundy: " + roundType);
        }
        return questions.get(random.nextInt(questions.size()));
    }

    public List<QuestionOption> getOptions(Question question) {
        List<QuestionOption> options = optionRepository.findByQuestionIdOrderByDisplayOrderAsc(question.getId());
        if (options.size() < 4) {
            throw new IllegalStateException("Pytanie klasyczne musi mieć 4 opcje odpowiedzi");
        }
        return options;
    }

    public Question createSessionQuestion(GameSession session, String content, RoundType roundType) {
        Question question = new Question(content, roundType, null, false, session);
        return questionRepository.save(question);
    }

    public List<QuestionCategory> findCategories(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        return categoryRepository.findAllById(ids);
    }

    public List<QuestionCategory> getCategories() {
        return categoryRepository.findAll();
    }
}
