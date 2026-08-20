package com.kinloop.backend.dto.profile;

import java.util.List;

public record DailyTimeBudgetProfileResponse(
        String questionCode,
        String question,
        String selectedOptionCode,
        Short dailyTimeBudgetMinutes,
        List<DailyTimeBudgetOptionResponse> options
) {
}
