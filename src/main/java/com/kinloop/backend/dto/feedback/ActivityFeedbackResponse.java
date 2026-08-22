package com.kinloop.backend.dto.feedback;

import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.FeedbackReason;
import com.kinloop.backend.entity.enums.FeedbackType;
import java.math.BigDecimal;

public record ActivityFeedbackResponse(
        Long feedbackId,
        Long dailyPlanItemId,
        FeedbackType feedbackType,
        FeedbackReason resolvedReason,
        DevelopmentDomain domain,
        short domainLevel,
        BigDecimal domainStreak
) {
}
