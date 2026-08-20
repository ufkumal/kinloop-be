package com.kinloop.backend.dto.profile;

import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.InvolvementType;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ActivityHistoryItemResponse(
        Long dailyPlanItemId,
        Long activityId,
        String title,
        String description,
        short durationMinutes,
        DevelopmentDomain targetDomain,
        InvolvementType involvementType,
        LocalDate planDate,
        OffsetDateTime selectedAt,
        OffsetDateTime completedAt,
        boolean completed
) {
}
