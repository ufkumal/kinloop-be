package com.kinloop.backend.dto.home;

import java.time.OffsetDateTime;

public record HomeActivityResponse(
        Long dailyPlanItemId,
        Long activityId,
        String title,
        String description,
        short durationMinutes,
        String slotType,
        String intro,
        String purpose,
        String whyItMatters,
        String easierVariation,
        String harderVariation,
        String observationTip,
        OffsetDateTime selectedAt,
        OffsetDateTime completedAt,
        boolean feedbackSubmitted,
        HomeFeedbackResponse feedback
) {
}
