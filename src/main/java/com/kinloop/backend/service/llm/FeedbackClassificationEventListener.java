package com.kinloop.backend.service.llm;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Defers the LLM call until the triggering vote's transaction has committed, and runs it
 * off the request thread, so submitting feedback never waits on model latency
 * (Kidloop_FewShot_Prompt_v2.md §11.2).
 */
@org.springframework.stereotype.Component
@RequiredArgsConstructor
public class FeedbackClassificationEventListener {
    private final FeedbackClassificationService classificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFeedbackSubmitted(FeedbackSubmittedEvent event) {
        classificationService.classifyAndApply(event.feedbackId());
    }
}
