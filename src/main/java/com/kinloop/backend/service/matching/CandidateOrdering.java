package com.kinloop.backend.service.matching;

import com.kinloop.backend.entity.ChildIntelligenceScore;
import com.kinloop.backend.entity.enums.IntelligenceType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CandidateOrdering {

    public Comparator<ScoredActivity> comparator(
            long childId,
            LocalDate planDate,
            Map<IntelligenceType, ChildIntelligenceScore> intelligenceScores,
            Map<String, BigDecimal> parameters
    ) {
        return Comparator.comparing(ScoredActivity::rawScore, Comparator.reverseOrder())
                .thenComparingInt(candidate -> sampleCount(candidate, intelligenceScores))
                .thenComparingInt(this::totalSensoryLoad)
                .thenComparingInt(candidate -> candidate.activity().getDurationMinutes())
                .thenComparing(candidate -> seed(childId, planDate, candidate.activity().getId(), parameters));
    }

    BigInteger seed(long childId, LocalDate planDate, long activityId, Map<String, BigDecimal> parameters) {
        BigInteger seedA = parameters.get("tiebreak_seed_a").toBigIntegerExact();
        BigInteger seedB = parameters.get("tiebreak_seed_b").toBigIntegerExact();
        BigInteger modulus = parameters.get("tiebreak_seed_mod").toBigIntegerExact();
        if (modulus.signum() <= 0) throw new IllegalArgumentException("tiebreak_seed_mod must be positive");

        long day = planDate.getYear() * 10_000L + planDate.getMonthValue() * 100L + planDate.getDayOfMonth();
        return BigInteger.valueOf(childId).multiply(seedA)
                .add(BigInteger.valueOf(day).multiply(seedB))
                .add(BigInteger.valueOf(activityId))
                .mod(modulus);
    }

    private int sampleCount(
            ScoredActivity candidate,
            Map<IntelligenceType, ChildIntelligenceScore> intelligenceScores
    ) {
        ChildIntelligenceScore score = intelligenceScores.get(candidate.activity().getTargetIntelligence());
        if (score == null) {
            throw new IllegalStateException("Missing intelligence score: " + candidate.activity().getTargetIntelligence());
        }
        return score.getFeedbackCount();
    }

    private int totalSensoryLoad(ScoredActivity candidate) {
        return candidate.activity().getNoiseLoad()
                + candidate.activity().getVisualLoad()
                + candidate.activity().getPhysicalIntensity();
    }
}
