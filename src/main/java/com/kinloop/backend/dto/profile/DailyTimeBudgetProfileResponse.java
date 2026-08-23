package com.kinloop.backend.dto.profile;

import java.util.List;

public record DailyTimeBudgetProfileResponse(
        String questionCode,
        String question,
        String selectedOptionCode,
        short minMinutes,
        short maxMinutes,
        List<DailyTimeBudgetOptionResponse> options
) {
}
