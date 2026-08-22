package com.kinloop.backend.service.matching;

import static com.kinloop.backend.service.matching.MatchingTestFixtures.activity;
import static com.kinloop.backend.service.matching.MatchingTestFixtures.neutralIntelligenceScores;
import static com.kinloop.backend.service.matching.MatchingTestFixtures.parameters;
import static com.kinloop.backend.service.matching.MatchingTestFixtures.set;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.ChildIntelligenceScore;
import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.IntelligenceType;
import com.kinloop.backend.entity.enums.InvolvementType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CandidateOrderingTest {
    private final CandidateOrdering ordering = new CandidateOrdering();

    @Test
    void appliesEveryV6OrderingStageInSequence() {
        Map<IntelligenceType, ChildIntelligenceScore> scores = neutralIntelligenceScores();
        set(scores.get(IntelligenceType.VERBAL_LINGUISTIC), "feedbackCount", 3);
        set(scores.get(IntelligenceType.LOGICAL_MATHEMATICAL), "feedbackCount", 1);

        ScoredActivity highestRaw = scored(1L, "101", IntelligenceType.VERBAL_LINGUISTIC, 9, 20);
        ScoredActivity leastSampled = scored(2L, "100", IntelligenceType.LOGICAL_MATHEMATICAL, 9, 20);
        ScoredActivity lowestLoad = scored(3L, "100", IntelligenceType.VERBAL_LINGUISTIC, 3, 20);
        ScoredActivity shortest = scored(4L, "100", IntelligenceType.VERBAL_LINGUISTIC, 6, 5);
        ScoredActivity longest = scored(5L, "100", IntelligenceType.VERBAL_LINGUISTIC, 6, 10);

        List<ScoredActivity> candidates = new ArrayList<>(
                List.of(longest, shortest, lowestLoad, leastSampled, highestRaw));
        candidates.sort(ordering.comparator(42L, LocalDate.of(2026, 8, 21), scores, parameters()));

        assertEquals(List.of(1L, 2L, 3L, 4L, 5L),
                candidates.stream().map(candidate -> candidate.activity().getId()).toList());
    }

    @Test
    void seedUsesExactOverflowSafeArithmetic() {
        LocalDate date = LocalDate.of(2026, 8, 21);
        BigInteger actual = ordering.seed(Long.MAX_VALUE, date, Long.MAX_VALUE, parameters());
        BigInteger expected = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(1_000_003L))
                .add(BigInteger.valueOf(20_260_821L).multiply(BigInteger.valueOf(10_007L)))
                .add(BigInteger.valueOf(Long.MAX_VALUE))
                .mod(BigInteger.valueOf(2_147_483_647L));

        assertEquals(expected, actual);
    }

    @Test
    void finalTieBreakOrdersCandidatesByAscendingSeed() {
        LocalDate date = LocalDate.of(2026, 8, 21);
        ScoredActivity first = scored(10L, "100", IntelligenceType.VERBAL_LINGUISTIC, 3, 10);
        ScoredActivity second = scored(20L, "100", IntelligenceType.VERBAL_LINGUISTIC, 3, 10);
        List<ScoredActivity> candidates = new ArrayList<>(List.of(second, first));

        candidates.sort(ordering.comparator(42L, date, neutralIntelligenceScores(), parameters()));

        List<Long> expected = List.of(first, second).stream()
                .sorted(java.util.Comparator.comparing(candidate ->
                        ordering.seed(42L, date, candidate.activity().getId(), parameters())))
                .map(candidate -> candidate.activity().getId())
                .toList();
        assertEquals(expected, candidates.stream().map(candidate -> candidate.activity().getId()).toList());
    }

    private ScoredActivity scored(long id, String raw, IntelligenceType intelligence, int load, int duration) {
        Activity activity = activity(id, DevelopmentDomain.LANGUAGE, 1, (short) duration,
                load / 3, load / 3, load - 2 * (load / 3), InvolvementType.BIRLIKTE, true);
        set(activity, "targetIntelligence", intelligence);
        BigDecimal score = new BigDecimal(raw);
        return new ScoredActivity(activity, score, score, Map.of());
    }
}
