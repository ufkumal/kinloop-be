package com.kinloop.backend.dto.home;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinloop.backend.entity.enums.OnboardingStep;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HomeStatusResponse(
        String state,
        Long childId,
        String childName,
        HomeChildResponse child,
        OnboardingStep onboardingStep,
        String nextQuestionCode,
        Long nextConsentId,
        HomeActivityResponse latestActivity,
        Boolean shouldGenerateDailyPlan
) {

    public static HomeStatusResponse newUser() {
        return new HomeStatusResponse("new-user", null, null, null, null, null, null, null, null);
    }

    public static HomeStatusResponse returningUser() {
        return new HomeStatusResponse("returning-user", null, null, null, null, null, null, null, null);
    }

    public static HomeStatusResponse halfOnboardingUser(
            HomeChildResponse child,
            OnboardingStep onboardingStep,
            String nextQuestionCode,
            Long nextConsentId
    ) {
        return new HomeStatusResponse(
                "half-onboarding-user", child.childId(), child.displayName(), child,
                onboardingStep, nextQuestionCode, nextConsentId, null, false);
    }

    public static HomeStatusResponse returningUser(Long childId, String childName,
                                                   HomeActivityResponse latestActivity,
                                                   boolean shouldGenerateDailyPlan) {
        return new HomeStatusResponse(
                "returning-user", childId, childName, null, null, null, null,
                latestActivity, shouldGenerateDailyPlan);
    }

    public static HomeStatusResponse feedbackRequired(Long childId, String childName,
                                                      HomeActivityResponse latestActivity) {
        return new HomeStatusResponse(
                "feedback-required", childId, childName, null, null, null, null, latestActivity, false);
    }
}
