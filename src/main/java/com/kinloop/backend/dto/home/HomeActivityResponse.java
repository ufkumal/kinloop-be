package com.kinloop.backend.dto.home;

import java.time.OffsetDateTime;

public record HomeActivityResponse(
        Long dailyPlanItemId,
        Long activityId,
        String title,
        OffsetDateTime selectedAt
) {
}
