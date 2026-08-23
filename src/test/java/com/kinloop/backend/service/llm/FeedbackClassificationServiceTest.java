package com.kinloop.backend.service.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.DailyPlan;
import com.kinloop.backend.entity.DailyPlanItem;
import com.kinloop.backend.entity.Feedback;
import com.kinloop.backend.entity.FeedbackLlmClassification;
import com.kinloop.backend.entity.ParentProfile;
import com.kinloop.backend.entity.enums.FeedbackType;
import com.kinloop.backend.entity.enums.IntelligenceType;
import com.kinloop.backend.entity.enums.PlanSlotType;
import com.kinloop.backend.entity.enums.SituationHint;
import com.kinloop.backend.repository.ChildRepository;
import com.kinloop.backend.repository.FeedbackLlmClassificationRepository;
import com.kinloop.backend.repository.FeedbackRepository;
import com.kinloop.backend.repository.ParentProfileRepository;
import com.kinloop.backend.service.ConsentService;
import com.kinloop.backend.service.FeedbackLearningService;
import com.kinloop.backend.service.matching.MatchingParameters;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FeedbackClassificationServiceTest {
    @Mock private FeedbackRepository feedbackRepository;
    @Mock private FeedbackLlmClassificationRepository classificationRepository;
    @Mock private ChildRepository childRepository;
    @Mock private ParentProfileRepository parentProfileRepository;
    @Mock private ConsentService consentService;
    @Mock private LlmProperties llmProperties;
    @Mock private AnthropicMessagesClient client;
    @Mock private FeedbackClassificationParser parser;
    @Mock private MatchingParameters matchingParameters;
    @Mock private FeedbackLearningService feedbackLearningService;
    @InjectMocks private FeedbackClassificationService service;

    private Feedback feedback;
    private Child child;
    private ParentProfile parentProfile;

    @BeforeEach
    void setUp() {
        Activity activity = new Activity();
        ReflectionTestUtils.setField(activity, "title", "Yaprak tepsisi");
        ReflectionTestUtils.setField(activity, "targetIntelligence", IntelligenceType.NATURALISTIC);
        ReflectionTestUtils.setField(activity, "targetDomain",
                com.kinloop.backend.entity.enums.DevelopmentDomain.COGNITIVE);
        DailyPlan plan = new DailyPlan(7L, LocalDate.now());
        plan.add(activity, PlanSlotType.DEVELOP, BigDecimal.TEN);
        DailyPlanItem item = plan.getItems().getFirst();
        feedback = new Feedback(7L, item, FeedbackType.LIKED, null, "Konuştu, hikaye anlattı");
        ReflectionTestUtils.setField(feedback, "id", 91L);

        child = new Child();
        child.setId(7L);
        child.setParentId(3L);
        parentProfile = new ParentProfile();
        parentProfile.setId(3L);
        parentProfile.setUserId(1L);
    }

    @Test
    void doesNothingWhenFeedbackHasNoFreeText() {
        Feedback noText = new Feedback(7L, feedback.getDailyPlanItem(), FeedbackType.LIKED, null, null);
        when(feedbackRepository.findById(91L)).thenReturn(Optional.of(noText));

        service.classifyAndApply(91L);

        verify(client, never()).complete(any(), any());
    }

    @Test
    void doesNothingWhenLlmIsDisabled() {
        when(feedbackRepository.findById(91L)).thenReturn(Optional.of(feedback));
        when(llmProperties.isEnabled()).thenReturn(false);

        service.classifyAndApply(91L);

        verify(client, never()).complete(any(), any());
        verify(classificationRepository, never()).save(any());
    }

    @Test
    void doesNothingWhenConsentIsMissing() {
        when(feedbackRepository.findById(91L)).thenReturn(Optional.of(feedback));
        when(llmProperties.isEnabled()).thenReturn(true);
        when(childRepository.findById(7L)).thenReturn(Optional.of(child));
        when(parentProfileRepository.findById(3L)).thenReturn(Optional.of(parentProfile));
        when(consentService.hasGrantedConsent(1L, com.kinloop.backend.entity.enums.ConsentType.DATA_PROCESSING))
                .thenReturn(false);

        service.classifyAndApply(91L);

        verify(client, never()).complete(any(), any());
        verify(classificationRepository, never()).save(any());
    }

    @Test
    void savesUnappliedRowWhenModelResponseIsInvalid() {
        givenEnabledAndConsented();
        when(client.complete(any(), any())).thenReturn("not json");
        when(parser.parse("not json")).thenReturn(ParsedClassification.invalid());

        service.classifyAndApply(91L);

        ArgumentCaptor<FeedbackLlmClassification> captor = ArgumentCaptor.forClass(FeedbackLlmClassification.class);
        verify(classificationRepository).save(captor.capture());
        assertFalse(captor.getValue().isApplied());
        verify(feedbackLearningService, never()).reapplyGardnerLearningWithOverride(anyLong(), any(), any());
    }

    @Test
    void savesUnappliedRowWhenConfidenceBelowThreshold() {
        givenEnabledAndConsented();
        ParsedClassification low = new ParsedClassification(
                true, new BigDecimal("0.50"), IntelligenceType.VERBAL_LINGUISTIC, null,
                null, null, null, null, null, false);
        when(client.complete(any(), any())).thenReturn("{}");
        when(parser.parse("{}")).thenReturn(low);
        when(matchingParameters.load()).thenReturn(Map.of(
                "llm_feedback_confidence_threshold", new BigDecimal("0.70")));

        service.classifyAndApply(91L);

        ArgumentCaptor<FeedbackLlmClassification> captor = ArgumentCaptor.forClass(FeedbackLlmClassification.class);
        verify(classificationRepository).save(captor.capture());
        assertFalse(captor.getValue().isApplied());
        verify(feedbackLearningService, never()).reapplyGardnerLearningWithOverride(anyLong(), any(), any());
    }

    @Test
    void transientSituationReversesButtonEffectsInsteadOfApplyingNewOnes() {
        givenEnabledAndConsented();
        ParsedClassification transientResult = new ParsedClassification(
                true, new BigDecimal("0.85"), null, null,
                null, null, null, SituationHint.TRANSIENT, null, false);
        when(client.complete(any(), any())).thenReturn("{}");
        when(parser.parse("{}")).thenReturn(transientResult);
        when(matchingParameters.load()).thenReturn(Map.of(
                "llm_feedback_confidence_threshold", new BigDecimal("0.70")));

        service.classifyAndApply(91L);

        verify(feedbackLearningService).reverseEffects(91L);
        verify(feedbackLearningService, never()).reapplyGardnerLearningWithOverride(anyLong(), any(), any());
        ArgumentCaptor<FeedbackLlmClassification> captor = ArgumentCaptor.forClass(FeedbackLlmClassification.class);
        verify(classificationRepository).save(captor.capture());
        assertTrue(captor.getValue().isApplied());
    }

    @Test
    void targetCorrectionRedirectsGardnerCreditViaReapply() {
        givenEnabledAndConsented();
        ParsedClassification result = new ParsedClassification(
                true, new BigDecimal("0.85"), IntelligenceType.VERBAL_LINGUISTIC, null,
                null, null, null, null, null, false);
        when(client.complete(any(), any())).thenReturn("{}");
        when(parser.parse("{}")).thenReturn(result);
        when(matchingParameters.load()).thenReturn(Map.of(
                "llm_feedback_confidence_threshold", new BigDecimal("0.70")));

        service.classifyAndApply(91L);

        verify(feedbackLearningService).reapplyGardnerLearningWithOverride(
                91L, IntelligenceType.VERBAL_LINGUISTIC, null);
        ArgumentCaptor<FeedbackLlmClassification> captor = ArgumentCaptor.forClass(FeedbackLlmClassification.class);
        verify(classificationRepository).save(captor.capture());
        assertTrue(captor.getValue().isApplied());
    }

    @Test
    void appliesAllPresentFilterAndDifficultyHints() {
        givenEnabledAndConsented();
        ParsedClassification result = new ParsedClassification(
                true, new BigDecimal("0.85"), null, null,
                com.kinloop.backend.entity.enums.SensoryHint.NOISE,
                com.kinloop.backend.entity.enums.InvolvementHint.TOGETHER,
                com.kinloop.backend.entity.enums.DifficultyHint.HARDER,
                null, null, false);
        when(client.complete(any(), any())).thenReturn("{}");
        when(parser.parse("{}")).thenReturn(result);
        when(matchingParameters.load()).thenReturn(Map.of(
                "llm_feedback_confidence_threshold", new BigDecimal("0.70")));

        service.classifyAndApply(91L);

        verify(feedbackLearningService).applySensoryHint(7L, com.kinloop.backend.entity.enums.SensoryHint.NOISE);
        verify(feedbackLearningService).applyInvolvementHint(
                7L, com.kinloop.backend.entity.enums.InvolvementHint.TOGETHER);
        verify(feedbackLearningService).applyDifficultyHint(
                7L, com.kinloop.backend.entity.enums.DevelopmentDomain.COGNITIVE,
                com.kinloop.backend.entity.enums.DifficultyHint.HARDER);
    }

    private void givenEnabledAndConsented() {
        when(feedbackRepository.findById(91L)).thenReturn(Optional.of(feedback));
        when(llmProperties.isEnabled()).thenReturn(true);
        when(childRepository.findById(7L)).thenReturn(Optional.of(child));
        when(parentProfileRepository.findById(3L)).thenReturn(Optional.of(parentProfile));
        when(consentService.hasGrantedConsent(1L, com.kinloop.backend.entity.enums.ConsentType.DATA_PROCESSING))
                .thenReturn(true);
    }
}
