package com.kinloop.backend.dto.matching;

import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DailyPlanResponse(
        Long planId,
        Long childId,
        LocalDate planDate,
        int budgetMin,
        int budgetMax,
        int committedDurationMinutes,
        int totalDurationMinutes,
        short fallbackLevel,
        List<DailyActivityResponse> activities,
        String state,
        String message,
        boolean showOnboardingReminder
) {
}
