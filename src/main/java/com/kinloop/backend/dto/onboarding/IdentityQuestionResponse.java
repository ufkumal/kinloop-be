package com.kinloop.backend.dto.onboarding;

import com.kinloop.backend.entity.enums.QuestionType;

public record IdentityQuestionResponse(
        String code,
        int displayOrder,
        String body,
        QuestionType type,
        boolean required) {
}
