package com.kinloop.backend.service.llm;

/**
 * Result of one synchronous classification attempt. {@code attempted=false} means the
 * LLM was disabled, consent was missing, or the provider call failed; ordinary button
 * learning must continue unchanged in every one of those cases.
 */
public record FeedbackClassificationOutcome(
        boolean attempted,
        String modelName,
        String rawResponse,
        ParsedClassification classification
) {
    public static FeedbackClassificationOutcome notAttempted() {
        return new FeedbackClassificationOutcome(false, null, null, ParsedClassification.invalid());
    }

    public static FeedbackClassificationOutcome completed(
            String modelName,
            String rawResponse,
            ParsedClassification classification
    ) {
        return new FeedbackClassificationOutcome(true, modelName, rawResponse, classification);
    }
}
