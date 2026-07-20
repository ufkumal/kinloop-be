package com.kinloop.backend.service;

import com.kinloop.backend.dto.onboarding.IdentityQuestionResponse;
import com.kinloop.backend.dto.onboarding.IdentityQuestionsResponse;
import com.kinloop.backend.entity.Question;
import com.kinloop.backend.entity.enums.QuestionScope;
import com.kinloop.backend.repository.QuestionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public IdentityQuestionsResponse getIdentityQuestions() {
        List<Question> questions = questionRepository.findByScopeAndActiveTrueOrderByDisplayOrderAsc(QuestionScope.IDENTITY);
        if (questions.isEmpty()) {
            throw new IllegalStateException("Identity questions are missing");
        }
        List<IdentityQuestionResponse> responses = questions.stream()
                .map(this::toResponse)
                .toList();
        return new IdentityQuestionsResponse(responses);
    }

    private IdentityQuestionResponse toResponse(Question question) {
        return new IdentityQuestionResponse(
                question.getCode(),
                question.getDisplayOrder(),
                question.getBody(),
                question.getQuestionType(),
                question.isRequired()
        );
    }
}
