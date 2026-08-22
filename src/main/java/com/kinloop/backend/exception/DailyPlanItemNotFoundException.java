package com.kinloop.backend.exception;

public class DailyPlanItemNotFoundException extends RuntimeException {
    public DailyPlanItemNotFoundException(Long dailyPlanItemId) {
        super("Daily plan item not found: " + dailyPlanItemId);
    }
}
