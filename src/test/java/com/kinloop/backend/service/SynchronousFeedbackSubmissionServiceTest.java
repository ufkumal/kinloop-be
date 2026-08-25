package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinloop.backend.dto.feedback.ActivityFeedbackResponse;
import com.kinloop.backend.dto.feedback.SubmitActivityFeedbackRequest;
import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.FeedbackType;
import com.kinloop.backend.repository.DailyPlanItemRepository;
import com.kinloop.backend.repository.FeedbackRepository;
import com.kinloop.backend.service.llm.FeedbackClassificationOutcome;
import com.kinloop.backend.service.llm.FeedbackClassificationService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SynchronousFeedbackSubmissionServiceTest {
    @Mock private DailyPlanItemRepository dailyPlanItemRepository;
    @Mock private FeedbackRepository feedbackRepository;
    @Mock private FeedbackClassificationService classificationService;
    @Mock private FeedbackLearningService feedbackLearningService;
    @InjectMocks private SynchronousFeedbackSubmissionService service;

    @Test
    void waitsForClassificationBeforeWritingAndReturningFeedback() {
        Child child = new Child();
        child.setId(7L);
        Activity activity = new Activity();
        FeedbackClassificationOutcome outcome = FeedbackClassificationOutcome.notAttempted();
        ActivityFeedbackResponse response = new ActivityFeedbackResponse(
                91L, 11L, FeedbackType.LIKED, null,
                DevelopmentDomain.LANGUAGE, (short) 2, BigDecimal.ZERO);
        when(feedbackRepository.existsByDailyPlanItemId(11L)).thenReturn(false);
        when(dailyPlanItemRepository.findActivityForFeedback(11L, 7L)).thenReturn(Optional.of(activity));
        when(classificationService.classify(7L, activity, FeedbackType.LIKED, "Hikaye anlattı"))
                .thenReturn(outcome);
        when(feedbackLearningService.submit(any(), any(), any(), any())).thenReturn(response);

        ActivityFeedbackResponse actual = service.submit(
                child, 11L,
                new SubmitActivityFeedbackRequest(FeedbackType.LIKED, "  Hikaye anlattı  "));

        assertEquals(response, actual);
        var order = inOrder(classificationService, feedbackLearningService);
        order.verify(classificationService).classify(
                7L, activity, FeedbackType.LIKED, "Hikaye anlattı");
        order.verify(feedbackLearningService).submit(any(), any(), any(), any());
    }

    @Test
    void blankTextUsesNormalFeedbackFlowWithoutCallingLlm() {
        Child child = new Child();
        child.setId(7L);
        ActivityFeedbackResponse response = new ActivityFeedbackResponse(
                91L, 11L, FeedbackType.STRUGGLED, null,
                DevelopmentDomain.LANGUAGE, (short) 2, BigDecimal.ZERO);
        when(feedbackLearningService.submit(any(), any(), any(), any())).thenReturn(response);

        service.submit(
                child, 11L,
                new SubmitActivityFeedbackRequest(FeedbackType.STRUGGLED, "  \t "));

        verify(classificationService, never()).classify(any(), any(), any(), any());
        verify(dailyPlanItemRepository, never()).findActivityForFeedback(any(), any());
    }
}
