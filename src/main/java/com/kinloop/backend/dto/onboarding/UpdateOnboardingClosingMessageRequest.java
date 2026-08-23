package com.kinloop.backend.dto.onboarding;

import com.kinloop.backend.entity.enums.OnboardingClosingAction;
import jakarta.validation.constraints.NotNull;

public record UpdateOnboardingClosingMessageRequest(@NotNull OnboardingClosingAction action) {
}
