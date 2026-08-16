package com.kinloop.backend.service.matching;

import com.kinloop.backend.entity.ChildIntelligenceScore;
import com.kinloop.backend.entity.enums.IntelligenceType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class StrengthenCandidateSelector {

    public List<ScoredActivity> select(
            List<ScoredActivity> scored,
            Map<IntelligenceType, ChildIntelligenceScore> scores,
            int limit) {
        BigDecimal maximum = scores.values().stream()
                .map(ChildIntelligenceScore::getScore)
                .max(BigDecimal::compareTo)
                .orElseThrow();

        List<IntelligenceType> strongest = scores.values().stream()
                .filter(score -> score.getScore().compareTo(maximum) == 0)
                .map(ChildIntelligenceScore::getIntelligenceType)
                .toList();

        if (strongest.size() > 1) {
            return scored.stream().limit(limit).toList();
        }

        IntelligenceType uniqueStrongest = strongest.getFirst();
        return scored.stream()
                .filter(activity -> activity.activity().getTargetIntelligence() == uniqueStrongest)
                .limit(limit)
                .toList();
    }
}
