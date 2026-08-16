package com.kinloop.backend.exception;

public class ActivityNotInDailyPlanException extends RuntimeException {
    public ActivityNotInDailyPlanException(Long activityId) {
        super("Activity is not in today's daily plan: " + activityId);
    }
}
