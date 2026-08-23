package com.kinloop.backend.dto.home;

import com.kinloop.backend.entity.enums.FeedbackReason;
import com.kinloop.backend.entity.enums.FeedbackType;
import java.time.OffsetDateTime;

public record HomeFeedbackResponse(
        Long feedbackId,
        FeedbackType feedbackType,
        FeedbackReason resolvedReason,
        String freeText,
        OffsetDateTime createdAt
) {
}
