package com.kinloop.backend.exception;

public class FeedbackAlreadySubmittedException extends RuntimeException {
    public FeedbackAlreadySubmittedException(Long dailyPlanItemId) {
        super("Feedback already exists for daily plan item " + dailyPlanItemId);
    }
}
