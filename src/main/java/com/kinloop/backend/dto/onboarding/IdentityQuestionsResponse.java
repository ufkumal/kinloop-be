package com.kinloop.backend.dto.onboarding;

import java.util.List;

public record IdentityQuestionsResponse(
        List<IdentityQuestionResponse> questions
) {
}
