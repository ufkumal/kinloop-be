package com.kinloop.backend.dto.onboarding;

import jakarta.validation.constraints.NotBlank;

public record UpdateDailyTimeBudgetRequest(
        @NotBlank String optionCode
) {
}
