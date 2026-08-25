package com.kinloop.backend.dto.feedback;

import com.kinloop.backend.entity.enums.FeedbackType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitActivityFeedbackRequest(
        @NotNull FeedbackType feedbackType,
        @Size(max = 500, message = "must be at most 500 characters") String freeText
) {
}
