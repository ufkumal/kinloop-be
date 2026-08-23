package com.kinloop.backend.service;

import com.kinloop.backend.dto.feedback.ActivityFeedbackResponse;
import com.kinloop.backend.dto.feedback.SubmitActivityFeedbackRequest;
import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.exception.DailyPlanItemNotFoundException;
import com.kinloop.backend.exception.FeedbackAlreadySubmittedException;
import com.kinloop.backend.repository.DailyPlanItemRepository;
import com.kinloop.backend.repository.FeedbackRepository;
import com.kinloop.backend.service.llm.FeedbackClassificationOutcome;
import com.kinloop.backend.service.llm.FeedbackClassificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Keeps the provider call synchronous while ensuring it runs outside the DB write transaction. */
@Service
@RequiredArgsConstructor
public class SynchronousFeedbackSubmissionService {
    private final DailyPlanItemRepository dailyPlanItemRepository;
    private final FeedbackRepository feedbackRepository;
    private final FeedbackClassificationService classificationService;
    private final FeedbackLearningService feedbackLearningService;

    public ActivityFeedbackResponse submit(
            Child child,
            Long dailyPlanItemId,
            SubmitActivityFeedbackRequest request
    ) {
        String freeText = normalizeFreeText(request.freeText());
        SubmitActivityFeedbackRequest normalizedRequest =
                new SubmitActivityFeedbackRequest(request.feedbackType(), freeText);

        FeedbackClassificationOutcome outcome = FeedbackClassificationOutcome.notAttempted();
        if (freeText != null) {
            // Avoid paying for a call that cannot be persisted. The transactional writer
            // repeats this check under its normal plan-item lock to close races.
            if (feedbackRepository.existsByDailyPlanItemId(dailyPlanItemId)) {
                throw new FeedbackAlreadySubmittedException(dailyPlanItemId);
            }
            Activity activity = dailyPlanItemRepository
                    .findActivityForFeedback(dailyPlanItemId, child.getId())
                    .orElseThrow(() -> new DailyPlanItemNotFoundException(dailyPlanItemId));
            outcome = classificationService.classify(
                    child.getId(), activity, request.feedbackType(), freeText);
        }

        return feedbackLearningService.submit(child, dailyPlanItemId, normalizedRequest, outcome);
    }

    private String normalizeFreeText(String freeText) {
        if (freeText == null) return null;
        String trimmed = freeText.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
