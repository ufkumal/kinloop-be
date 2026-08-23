package com.kinloop.backend.dto.home;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HomeStatusResponse(
        String state,
        Long childId,
        String childName,
        HomeActivityResponse latestActivity,
        Boolean shouldGenerateDailyPlan
) {

    public static HomeStatusResponse newUser() {
        return new HomeStatusResponse("new-user", null, null, null, null);
    }

    public static HomeStatusResponse returningUser() {
        return new HomeStatusResponse("returning-user", null, null, null, null);
    }

    public static HomeStatusResponse returningUser(Long childId, String childName,
                                                   HomeActivityResponse latestActivity,
                                                   boolean shouldGenerateDailyPlan) {
        return new HomeStatusResponse(
                "returning-user", childId, childName, latestActivity, shouldGenerateDailyPlan);
    }

    public static HomeStatusResponse feedbackRequired(Long childId, String childName,
                                                      HomeActivityResponse latestActivity) {
        return new HomeStatusResponse(
                "feedback-required", childId, childName, latestActivity, false);
    }
}
