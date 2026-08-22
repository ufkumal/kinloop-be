package com.kinloop.backend.dto.matching;

import java.math.BigDecimal;

public record DailyActivityResponse(
        Long activityId,
        String title,
        String description,
        short durationMinutes,
        String slotType,
        BigDecimal score,
        String intro,
        String purpose,
        String whyItMatters,
        String easierVariation,
        String harderVariation,
        String observationTip,
        boolean withinBudget,
        boolean repeatNotice,
        boolean selected
) {
}
