package com.kinloop.backend.service.matching;

import static com.kinloop.backend.service.matching.MatchingTestFixtures.activity;
import static com.kinloop.backend.service.matching.MatchingTestFixtures.neutralIntelligenceScores;
import static com.kinloop.backend.service.matching.MatchingTestFixtures.set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinloop.backend.entity.ChildIntelligenceScore;
import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.IntelligenceType;
import com.kinloop.backend.entity.enums.InvolvementType;
import com.kinloop.backend.entity.enums.PlanSlotType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DailyPortfolioBuilderTest {
    private final DailyPortfolioBuilder builder = new DailyPortfolioBuilder();

    @Test
    void fillsOrderedSlotsWithReserveStrongestFallbackAndLeastSampledExploration() {
        Map<IntelligenceType, ChildIntelligenceScore> scores = scores();
        ScoredActivity development = scored(1L, DevelopmentDomain.LANGUAGE,
                IntelligenceType.NATURALISTIC, 20, InvolvementType.BIRLIKTE, "120");
        ScoredActivity strongestAvailable = scored(2L, DevelopmentDomain.COGNITIVE,
                IntelligenceType.MUSICAL, 5, InvolvementType.BIRLIKTE, "110");
        ScoredActivity leastSampled = scored(3L, DevelopmentDomain.SENSORY,
                IntelligenceType.INTERPERSONAL, 5, InvolvementType.BIRLIKTE, "100");

        DailyPortfolioBuilder.Result result = builder.build(request(
                List.of(development, strongestAvailable, leastSampled),
                List.of(development, strongestAvailable, leastSampled), Set.of(), scores, 30, false));

        assertEquals((short) 0, result.fallbackLevel());
        assertEquals(List.of(PlanSlotType.DEVELOP, PlanSlotType.STRENGTHEN, PlanSlotType.EXPLORE),
                result.selections().stream().map(DailyPortfolioBuilder.Selection::slot).toList());
        assertEquals(List.of(1L, 2L, 3L), ids(result));
        assertEquals(30, result.committedDurationMinutes());
        assertEquals(30, result.totalDurationMinutes());
    }

    @Test
    void supervisedGuaranteeReplacesOnlyExplorationAndMarksOverflow() {
        Map<IntelligenceType, ChildIntelligenceScore> scores = neutralIntelligenceScores();
        ScoredActivity development = scored(11L, DevelopmentDomain.LANGUAGE,
                IntelligenceType.VERBAL_LINGUISTIC, 10, InvolvementType.BIRLIKTE, "120");
        ScoredActivity strengthening = scored(12L, DevelopmentDomain.COGNITIVE,
                IntelligenceType.LOGICAL_MATHEMATICAL, 10, InvolvementType.BIRLIKTE, "110");
        ScoredActivity exploration = scored(13L, DevelopmentDomain.SENSORY,
                IntelligenceType.MUSICAL, 5, InvolvementType.BIRLIKTE, "105");
        ScoredActivity supervised = scored(14L, DevelopmentDomain.FINE_MOTOR,
                IntelligenceType.INTERPERSONAL, 15, InvolvementType.GOZETIMLI, "90");
        List<ScoredActivity> pool = List.of(development, strengthening, exploration, supervised);

        DailyPortfolioBuilder.Result result = builder.build(
                request(pool, pool, Set.of(), scores, 25, true));

        assertEquals(List.of(11L, 12L, 14L), ids(result));
        DailyPortfolioBuilder.Selection replaced = result.selections().get(2);
        assertEquals(PlanSlotType.EXPLORE, replaced.slot());
        assertFalse(replaced.withinBudget());
        assertEquals(20, result.committedDurationMinutes());
        assertEquals(35, result.totalDurationMinutes());
    }

    @Test
    void supervisedGuaranteePrefersTheHighestOrderedFittingCandidate() {
        Map<IntelligenceType, ChildIntelligenceScore> scores = neutralIntelligenceScores();
        ScoredActivity development = scored(15L, DevelopmentDomain.LANGUAGE,
                IntelligenceType.VERBAL_LINGUISTIC, 10, InvolvementType.BIRLIKTE, "120");
        ScoredActivity strengthening = scored(16L, DevelopmentDomain.COGNITIVE,
                IntelligenceType.LOGICAL_MATHEMATICAL, 10, InvolvementType.BIRLIKTE, "110");
        ScoredActivity exploration = scored(17L, DevelopmentDomain.SENSORY,
                IntelligenceType.MUSICAL, 5, InvolvementType.BIRLIKTE, "105");
        ScoredActivity supervisedTooLong = scored(18L, DevelopmentDomain.FINE_MOTOR,
                IntelligenceType.INTERPERSONAL, 15, InvolvementType.GOZETIMLI, "100");
        ScoredActivity supervisedFitting = scored(19L, DevelopmentDomain.SOCIAL_EMOTIONAL,
                IntelligenceType.INTRAPERSONAL, 5, InvolvementType.GOZETIMLI, "90");
        List<ScoredActivity> pool = List.of(
                development, strengthening, exploration, supervisedTooLong, supervisedFitting);

        DailyPortfolioBuilder.Result result = builder.build(
                request(pool, pool, Set.of(), scores, 25, true));

        assertEquals(List.of(15L, 16L, 19L), ids(result));
        assertTrue(result.selections().get(2).withinBudget());
        assertEquals(25, result.committedDurationMinutes());
        assertEquals(25, result.totalDurationMinutes());
    }

    @Test
    void fallbackOneRetriesWithoutFreshnessAndMarksOnlyRepeatedItems() {
        Map<IntelligenceType, ChildIntelligenceScore> scores = neutralIntelligenceScores();
        ScoredActivity development = scored(21L, DevelopmentDomain.LANGUAGE,
                IntelligenceType.VERBAL_LINGUISTIC, 10, InvolvementType.BIRLIKTE, "120");
        ScoredActivity strengthening = scored(22L, DevelopmentDomain.COGNITIVE,
                IntelligenceType.LOGICAL_MATHEMATICAL, 10, InvolvementType.BIRLIKTE, "110");
        ScoredActivity exploration = scored(23L, DevelopmentDomain.SENSORY,
                IntelligenceType.MUSICAL, 5, InvolvementType.BIRLIKTE, "100");
        List<ScoredActivity> full = List.of(development, strengthening, exploration);

        DailyPortfolioBuilder.Result result = builder.build(
                request(List.of(strengthening, exploration), full, Set.of(21L), scores, 30, false));

        assertEquals((short) 1, result.fallbackLevel());
        assertTrue(result.selections().getFirst().repeatNotice());
        assertFalse(result.selections().get(1).repeatNotice());
        assertEquals(List.of((short) 1), result.warnings().stream()
                .map(DailyPortfolioBuilder.Warning::fallbackLevel).toList());
    }

    @Test
    void fallbackTwoRelaxesSlotsAndFallbackThreePermitsPartialPlans() {
        Map<IntelligenceType, ChildIntelligenceScore> scores = neutralIntelligenceScores();
        List<ScoredActivity> noDevelopmentDomain = List.of(
                scored(31L, DevelopmentDomain.COGNITIVE, IntelligenceType.VERBAL_LINGUISTIC,
                        10, InvolvementType.BIRLIKTE, "120"),
                scored(32L, DevelopmentDomain.SENSORY, IntelligenceType.LOGICAL_MATHEMATICAL,
                        10, InvolvementType.BIRLIKTE, "110"),
                scored(33L, DevelopmentDomain.FINE_MOTOR, IntelligenceType.MUSICAL,
                        5, InvolvementType.BIRLIKTE, "100"));

        DailyPortfolioBuilder.Result relaxed = builder.build(request(
                noDevelopmentDomain, noDevelopmentDomain, Set.of(), scores, 30, false));
        DailyPortfolioBuilder.Result partial = builder.build(request(
                noDevelopmentDomain.subList(0, 2), noDevelopmentDomain.subList(0, 2),
                Set.of(), scores, 30, false));

        assertEquals((short) 2, relaxed.fallbackLevel());
        assertEquals(3, relaxed.selections().size());
        assertEquals((short) 3, partial.fallbackLevel());
        assertEquals(2, partial.selections().size());
    }

    @Test
    void fallbackFourReturnsExplanatoryEmptyStateDataAndAllWarnings() {
        DailyPortfolioBuilder.Result result = builder.build(request(
                List.of(), List.of(), Set.of(), neutralIntelligenceScores(), 35, false));

        assertEquals((short) 4, result.fallbackLevel());
        assertTrue(result.selections().isEmpty());
        assertEquals(List.of((short) 1, (short) 2, (short) 3, (short) 4),
                result.warnings().stream().map(DailyPortfolioBuilder.Warning::fallbackLevel).toList());
    }

    private DailyPortfolioBuilder.Request request(
            List<ScoredActivity> fresh,
            List<ScoredActivity> all,
            Set<Long> recent,
            Map<IntelligenceType, ChildIntelligenceScore> scores,
            int budget,
            boolean guarantee
    ) {
        return new DailyPortfolioBuilder.Request(
                fresh, all, recent, DevelopmentDomain.LANGUAGE, scores, budget, guarantee);
    }

    private Map<IntelligenceType, ChildIntelligenceScore> scores() {
        Map<IntelligenceType, ChildIntelligenceScore> scores = neutralIntelligenceScores();
        set(scores.get(IntelligenceType.VERBAL_LINGUISTIC), "score", new BigDecimal("5.00"));
        set(scores.get(IntelligenceType.MUSICAL), "score", new BigDecimal("4.00"));
        for (ChildIntelligenceScore score : scores.values()) set(score, "feedbackCount", 2);
        set(scores.get(IntelligenceType.INTERPERSONAL), "feedbackCount", 0);
        return scores;
    }

    private ScoredActivity scored(
            long id,
            DevelopmentDomain domain,
            IntelligenceType intelligence,
            int duration,
            InvolvementType involvement,
            String score
    ) {
        var activity = activity(id, domain, 2, (short) duration,
                2, 2, 2, involvement, true);
        set(activity, "targetIntelligence", intelligence);
        BigDecimal value = new BigDecimal(score);
        return new ScoredActivity(activity, value, value.min(new BigDecimal("100")), Map.of());
    }

    private List<Long> ids(DailyPortfolioBuilder.Result result) {
        return result.selections().stream()
                .map(selection -> selection.activity().activity().getId())
                .toList();
    }
}
