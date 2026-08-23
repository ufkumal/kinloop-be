package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.ChildIntelligenceScore;
import com.kinloop.backend.entity.DailyPlan;
import com.kinloop.backend.entity.DailyPlanItem;
import com.kinloop.backend.entity.Feedback;
import com.kinloop.backend.entity.FeedbackEffect;
import com.kinloop.backend.entity.enums.FeedbackType;
import com.kinloop.backend.entity.enums.IntelligenceType;
import com.kinloop.backend.entity.enums.PlanSlotType;
import com.kinloop.backend.repository.ChildIntelligenceScoreRepository;
import com.kinloop.backend.repository.FeedbackEffectRepository;
import com.kinloop.backend.repository.FeedbackRepository;
import com.kinloop.backend.service.matching.MatchingParameters;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The button already applied +0.30/+0.15 to the activity's own target/secondary
 * intelligence when the LLM later returns a different target_correction. This verifies
 * the redirect: original fields end back at their pre-button score, and the override
 * fields end up credited instead — not on top of the original credit.
 */
@ExtendWith(MockitoExtension.class)
class FeedbackLearningServiceReapplyOverrideTest {
    @Mock private FeedbackRepository feedbackRepository;
    @Mock private FeedbackEffectRepository feedbackEffectRepository;
    @Mock private ChildIntelligenceScoreRepository intelligenceRepository;
    @Mock private MatchingParameters matchingParameters;
    @InjectMocks private FeedbackLearningService service;

    private Feedback feedback;
    private ChildIntelligenceScore originalTarget;
    private ChildIntelligenceScore originalSecondary;
    private ChildIntelligenceScore overrideTarget;
    private List<FeedbackEffect> buttonEffects;

    @BeforeEach
    void setUp() {
        Activity activity = new Activity();
        ReflectionTestUtils.setField(activity, "targetIntelligence", IntelligenceType.VERBAL_LINGUISTIC);
        ReflectionTestUtils.setField(activity, "secondaryIntelligence", IntelligenceType.MUSICAL);
        DailyPlan plan = new DailyPlan(7L, LocalDate.now());
        plan.add(activity, PlanSlotType.DEVELOP, BigDecimal.TEN);
        DailyPlanItem item = plan.getItems().getFirst();
        feedback = new Feedback(7L, item, FeedbackType.LIKED, null, "free text");
        ReflectionTestUtils.setField(feedback, "id", 91L);

        originalTarget = new ChildIntelligenceScore(7L, IntelligenceType.VERBAL_LINGUISTIC, new BigDecimal("3.30"));
        originalSecondary = new ChildIntelligenceScore(7L, IntelligenceType.MUSICAL, new BigDecimal("3.15"));
        overrideTarget = new ChildIntelligenceScore(7L, IntelligenceType.INTERPERSONAL, new BigDecimal("3.00"));

        buttonEffects = List.of(
                new FeedbackEffect(feedback, IntelligenceType.VERBAL_LINGUISTIC, new BigDecimal("0.30")),
                new FeedbackEffect(feedback, IntelligenceType.MUSICAL, new BigDecimal("0.15")));

        when(feedbackRepository.findById(91L)).thenReturn(Optional.of(feedback));
        when(feedbackEffectRepository.findByFeedbackIdAndReversedAtIsNull(91L)).thenReturn(buttonEffects);
        when(intelligenceRepository.findByChildId(7L)).thenReturn(
                List.of(originalTarget, originalSecondary, overrideTarget));
        when(matchingParameters.load()).thenReturn(Map.of(
                "liked_target_delta", new BigDecimal("0.30"),
                "liked_secondary_delta", new BigDecimal("0.15"),
                "gardner_runtime_min_score", BigDecimal.ZERO,
                "gardner_runtime_max_score", new BigDecimal("5.00")));
    }

    @Test
    void redirectsTargetCreditButLeavesUntouchedSecondaryAsItWas() {
        // secondaryOverride is null: the LLM only sent target_correction, so the
        // activity's own secondary (MUSICAL) is reversed and then re-credited unchanged
        // — net effect is only the target field moves.
        service.reapplyGardnerLearningWithOverride(91L, IntelligenceType.INTERPERSONAL, null);

        assertEquals(0, new BigDecimal("3.00").compareTo(originalTarget.getScore()));
        assertEquals(0, new BigDecimal("3.15").compareTo(originalSecondary.getScore()));
        assertEquals(0, new BigDecimal("3.30").compareTo(overrideTarget.getScore()));
        buttonEffects.forEach(effect -> org.junit.jupiter.api.Assertions.assertNotNull(effect.getReversedAt()));
    }

    @Test
    void savesNewEffectsForTheOverrideAndTheUnchangedSecondary() {
        service.reapplyGardnerLearningWithOverride(91L, IntelligenceType.INTERPERSONAL, null);

        // Two applyDelta() calls: one for the override target, one re-crediting the
        // activity's own (unredirected) secondary — see the test above for why.
        org.mockito.Mockito.verify(feedbackEffectRepository, org.mockito.Mockito.times(2)).save(any());
    }
}
