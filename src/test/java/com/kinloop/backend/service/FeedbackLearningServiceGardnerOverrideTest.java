package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.ChildIntelligenceScore;
import com.kinloop.backend.entity.DailyPlan;
import com.kinloop.backend.entity.DailyPlanItem;
import com.kinloop.backend.entity.Feedback;
import com.kinloop.backend.entity.enums.FeedbackType;
import com.kinloop.backend.entity.enums.IntelligenceType;
import com.kinloop.backend.entity.enums.PlanSlotType;
import com.kinloop.backend.repository.FeedbackEffectRepository;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FeedbackLearningServiceGardnerOverrideTest {
    private final AtomicInteger savedEffectCount = new AtomicInteger();
    private FeedbackLearningService service;
    private Activity activity;
    private Feedback feedback;

    @BeforeEach
    void setUp() {
        FeedbackEffectRepository effectRepository = (FeedbackEffectRepository) Proxy.newProxyInstance(
                FeedbackEffectRepository.class.getClassLoader(),
                new Class<?>[]{FeedbackEffectRepository.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("save")) {
                        savedEffectCount.incrementAndGet();
                        return arguments[0];
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        service = new FeedbackLearningService(
                null, null, effectRepository, null, null, null, null, null);

        activity = new Activity();
        ReflectionTestUtils.setField(activity, "targetIntelligence", IntelligenceType.VERBAL_LINGUISTIC);
        ReflectionTestUtils.setField(activity, "secondaryIntelligence", IntelligenceType.MUSICAL);
        DailyPlan plan = new DailyPlan(7L, LocalDate.now());
        plan.add(activity, PlanSlotType.DEVELOP, BigDecimal.TEN);
        DailyPlanItem item = plan.getItems().getFirst();
        feedback = new Feedback(7L, item, FeedbackType.LIKED, null, null);
        savedEffectCount.set(0);
    }

    @Test
    void nullOverridesPreserveActivityTargetAndSecondaryCredit() {
        ChildIntelligenceScore originalTarget = score(IntelligenceType.VERBAL_LINGUISTIC);
        ChildIntelligenceScore originalSecondary = score(IntelligenceType.MUSICAL);
        Map<IntelligenceType, ChildIntelligenceScore> scores = scores(originalTarget, originalSecondary);

        service.applyGardnerLearning(
                feedback, activity, FeedbackType.LIKED, null, scores, parameters(), null, null);

        assertScore("3.30", originalTarget);
        assertScore("3.15", originalSecondary);
        assertEquals(2, savedEffectCount.get());
    }

    @Test
    void overridesReplaceBothOriginalCreditsAndNeverCreditMoreThanTwoFields() {
        ChildIntelligenceScore originalTarget = score(IntelligenceType.VERBAL_LINGUISTIC);
        ChildIntelligenceScore originalSecondary = score(IntelligenceType.MUSICAL);
        ChildIntelligenceScore targetOverride = score(IntelligenceType.INTERPERSONAL);
        ChildIntelligenceScore secondaryOverride = score(IntelligenceType.NATURALISTIC);
        Map<IntelligenceType, ChildIntelligenceScore> scores = scores(
                originalTarget, originalSecondary, targetOverride, secondaryOverride);

        service.applyGardnerLearning(
                feedback,
                activity,
                FeedbackType.LIKED,
                null,
                scores,
                parameters(),
                IntelligenceType.INTERPERSONAL,
                IntelligenceType.NATURALISTIC);

        assertScore("3.00", originalTarget);
        assertScore("3.00", originalSecondary);
        assertScore("3.30", targetOverride);
        assertScore("3.15", secondaryOverride);
        assertEquals(0, originalTarget.getFeedbackCount());
        assertEquals(1, targetOverride.getFeedbackCount());
        assertEquals(2, savedEffectCount.get());
    }

    private ChildIntelligenceScore score(IntelligenceType type) {
        return new ChildIntelligenceScore(7L, type, new BigDecimal("3.00"));
    }

    private Map<IntelligenceType, ChildIntelligenceScore> scores(ChildIntelligenceScore... scores) {
        Map<IntelligenceType, ChildIntelligenceScore> values = new HashMap<>();
        for (ChildIntelligenceScore score : scores) {
            values.put(score.getIntelligenceType(), score);
        }
        return values;
    }

    private Map<String, BigDecimal> parameters() {
        return Map.of(
                "liked_target_delta", new BigDecimal("0.30"),
                "liked_secondary_delta", new BigDecimal("0.15"),
                "disliked_interest_delta", new BigDecimal("-0.15"),
                "gardner_runtime_min_score", BigDecimal.ZERO,
                "gardner_runtime_max_score", new BigDecimal("5.00"));
    }

    private void assertScore(String expected, ChildIntelligenceScore score) {
        assertEquals(0, new BigDecimal(expected).compareTo(score.getScore()));
    }
}
