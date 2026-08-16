package com.kinloop.backend.exception;

public class DailyPlanNotFoundException extends RuntimeException {
    public DailyPlanNotFoundException(Long childId) {
        super("Daily plan not found for child: " + childId);
    }
}
