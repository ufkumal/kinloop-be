package com.kinloop.backend.service.matching;

import com.kinloop.backend.entity.*;
import com.kinloop.backend.entity.enums.*;

import java.math.*;
import java.util.*;

import org.springframework.stereotype.Component;

@Component
public class ActivityScorer {
    public ScoredActivity score(Activity a, ChildProfileSnapshot profile, DunnProfile dunn, DevelopmentDomain period,
                                Map<IntelligenceType, ChildIntelligenceScore> scores, Map<DevelopmentDomain, ChildDomainLevel> levels,
                                Set<Long> yesterday, Map<String, BigDecimal> p) {
        double d = dunn.getNoiseWeight() == null ? 0 : dunn.getNoiseWeight().doubleValue() * Math.abs(dunn.getNoiseTolerance() - a.getNoiseLoad())
                                                       + dunn.getVisualWeight().doubleValue() * Math.abs(dunn.getVisualTolerance() - a.getVisualLoad())
                                                       + dunn.getMovementWeight().doubleValue() * Math.abs(dunn.getMovementTolerance() - a.getPhysicalIntensity());
        BigDecimal target = scores.get(a.getTargetIntelligence()).getScore();
        BigDecimal secondary = a.getSecondaryIntelligence() == null ? null : scores.get(a.getSecondaryIntelligence()).getScore();
        double g = target.compareTo(p.get("gardner_comfort_threshold")) >= 0 ? p.get("gardner_comfort_bonus").doubleValue()
                : target.compareTo(p.get("gardner_bridge_target_threshold")) <= 0 && secondary != null && secondary.compareTo(p.get("gardner_bridge_secondary_threshold")) >= 0 ? p.get("gardner_bridge_bonus").doubleValue()
                  : target.compareTo(p.get("gardner_block_threshold")) <= 0 ? p.get("gardner_block_penalty").doubleValue() : 0;
        double periodBonus = a.getTargetDomain() == period ? p.get("developmental_period_bonus").doubleValue() : 0;
        int level = levels.get(a.getTargetDomain()).getLevel();
        boolean supported = a.getInstruction() != null && a.getInstruction().getEasierVariation() != null && !a.getInstruction().getEasierVariation().isBlank();
        double z = a.getDifficulty() == level + 1 && supported ? p.get("zpd_sweet_spot_bonus").doubleValue() : a.getDifficulty() > level + 1 ? p.get("zpd_frustration_penalty").doubleValue() : a.getDifficulty() < level ? p.get("zpd_boredom_penalty").doubleValue() : 0;
        double b = profile.getSeparationAnxiety() != null && profile.getSeparationAnxiety() >= p.get("high_separation_anxiety_threshold").intValue() && a.getInvolvementType() != InvolvementType.BAGIMSIZ ? p.get("attachment_multiplier").doubleValue() : 1;
        double t = yesterday.contains(a.getId()) ? p.get("freshness_penalty").doubleValue() : 0;
        double raw = (p.get("score_base").doubleValue() - d + g + periodBonus + z) * b - t;
        double display = Math.max(p.get("score_display_min").doubleValue(), Math.min(p.get("score_display_max").doubleValue(), raw));
        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("base", p.get("score_base"));
        breakdown.put("D", d);
        breakdown.put("G", g);
        breakdown.put("P", periodBonus);
        breakdown.put("Z", z);
        breakdown.put("B", b);
        breakdown.put("T", t);
        breakdown.put("rawScore", raw);
        breakdown.put("displayScore", display);
        return new ScoredActivity(a, decimal(raw), decimal(display), breakdown);
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
