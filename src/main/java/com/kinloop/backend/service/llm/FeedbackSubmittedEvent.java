package com.kinloop.backend.service.llm;

/**
 * Published by FeedbackLearningService.submit() when free-text was captured.
 * Consumed after commit (see FeedbackClassificationEventListener) so the LLM
 * call never delays the synchronous vote-apply response.
 */
public record FeedbackSubmittedEvent(Long feedbackId) {
}
