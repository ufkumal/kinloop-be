package com.kinloop.backend.dto.feedback;

import com.kinloop.backend.entity.enums.FeedbackType;
import jakarta.validation.constraints.NotNull;

public record SubmitActivityFeedbackRequest(
        @NotNull FeedbackType feedbackType,
        String freeText
) {
}
