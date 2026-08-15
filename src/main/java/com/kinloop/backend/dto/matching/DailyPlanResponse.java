package com.kinloop.backend.dto.matching;

import java.time.LocalDate;
import java.util.List;

public record DailyPlanResponse(
        Long planId,
        Long childId,
        LocalDate planDate,
        short budgetMinutes,
        int totalDurationMinutes,
        List<DailyActivityResponse> activities
) {
}
