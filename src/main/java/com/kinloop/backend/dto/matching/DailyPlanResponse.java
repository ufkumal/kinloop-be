package com.kinloop.backend.dto.matching;

import java.time.LocalDate;
import java.util.List;

public record DailyPlanResponse(
        Long planId,
        Long childId,
        LocalDate planDate,
        int budgetMin,
        int budgetMax,
        int committedDurationMinutes,
        int totalDurationMinutes,
        short fallbackLevel,
        List<DailyActivityResponse> activities
) {
}
