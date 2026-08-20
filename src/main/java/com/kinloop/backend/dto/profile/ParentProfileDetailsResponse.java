package com.kinloop.backend.dto.profile;

public record ParentProfileDetailsResponse(
        Long parentProfileId,
        String email,
        String fullName,
        String phone,
        String city,
        String district,
        Short dailyTimeBudgetMinutes
) {
}
