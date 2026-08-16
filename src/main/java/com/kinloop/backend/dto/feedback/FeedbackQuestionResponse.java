package com.kinloop.backend.dto.feedback;

import com.kinloop.backend.dto.questionnaire.QuestionOptionResponse;
import com.kinloop.backend.entity.enums.QuestionType;
import java.util.List;

public record FeedbackQuestionResponse(
        String code,
        String body,
        String helperText,
        QuestionType type,
        boolean required,
        int displayOrder,
        Integer maxLength,
        List<QuestionOptionResponse> options
) {
}
