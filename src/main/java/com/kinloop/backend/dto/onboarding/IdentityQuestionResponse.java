package com.kinloop.backend.dto.onboarding;

import com.kinloop.backend.entity.enums.QuestionType;
import com.kinloop.backend.dto.questionnaire.QuestionOptionResponse;
import java.util.List;

public record IdentityQuestionResponse(
        String code,
        int displayOrder,
        String body,
        QuestionType type,
        boolean required,
        List<QuestionOptionResponse> options,
        String answeredOptionCode) {
}
