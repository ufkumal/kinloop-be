package com.kinloop.backend.dto.profile;

public record DailyTimeBudgetOptionResponse(
        String code,
        String label,
        int displayOrder,
        Short minutes
) {
}
