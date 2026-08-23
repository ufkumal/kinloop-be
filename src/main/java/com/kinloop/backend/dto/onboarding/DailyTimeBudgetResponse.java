package com.kinloop.backend.dto.onboarding;

public record DailyTimeBudgetResponse(String answeredOptionCode, short minMinutes, short maxMinutes) {
}
