package com.kinloop.backend.service;

import com.kinloop.backend.dto.feedback.FeedbackQuestionResponse;
import com.kinloop.backend.dto.feedback.FeedbackQuestionsResponse;
import com.kinloop.backend.dto.questionnaire.QuestionOptionResponse;
import com.kinloop.backend.entity.Question;
import com.kinloop.backend.entity.enums.QuestionScope;
import com.kinloop.backend.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackQuestionService {

    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public FeedbackQuestionsResponse questions() {
        return new FeedbackQuestionsResponse(questionRepository
                .findByScopeAndActiveTrueOrderByDisplayOrderAsc(QuestionScope.FEEDBACK)
                .stream()
                .map(this::response)
                .toList());
    }

    private FeedbackQuestionResponse response(Question question) {
        return new FeedbackQuestionResponse(
                question.getCode(),
                question.getBody(),
                question.getHelperText(),
                question.getQuestionType(),
                question.isRequired(),
                question.getDisplayOrder(),
                question.getMaxLength(),
                question.getOptions().stream()
                        .map(option -> new QuestionOptionResponse(
                                option.getCode(), option.getLabel(), option.getDisplayOrder()))
                        .toList()
        );
    }
}
