package com.kinloop.backend.dto.onboarding;

public record OnboardingClosingMessageResponse(
        boolean shouldDisplay,
        boolean planReminderEnabled,
        int reminderPlansRemaining
) {
}
