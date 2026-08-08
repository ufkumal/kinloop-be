package com.kinloop.backend.exception;

public class MissingDailyTimeBudgetException extends RuntimeException {
    public MissingDailyTimeBudgetException() {
        super("Daily time budget is required before completing onboarding");
    }
}
