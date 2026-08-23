package com.kinloop.backend.service.matching;

import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.ActivityInstruction;
import com.kinloop.backend.entity.ChildDomainLevel;
import com.kinloop.backend.entity.ChildIntelligenceScore;
import com.kinloop.backend.entity.ChildProfileSnapshot;
import com.kinloop.backend.entity.ChildSensoryAdjustment;
import com.kinloop.backend.entity.DunnProfile;
import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.IntelligenceType;
import com.kinloop.backend.entity.enums.InvolvementType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ActivityScorer {

    public ScoredActivity score(
            Activity activity,
            ChildProfileSnapshot profile,
            DunnProfile dunn,
            ChildSensoryAdjustment adjustment,
            DevelopmentDomain period,
            Map<IntelligenceType, ChildIntelligenceScore> scores,
            Map<DevelopmentDomain, ChildDomainLevel> levels,
            Map<String, BigDecimal> parameters
    ) {
        BigDecimal sensoryPenalty = sensoryPenalty(activity, dunn, adjustment);
        BigDecimal interestBonus = interestBonus(activity, scores, parameters);
        BigDecimal periodBonus = activity.getTargetDomain() == period
                ? parameters.get("developmental_period_bonus") : BigDecimal.ZERO;
        BigDecimal difficultyBonus = difficultyBonus(activity, levels, parameters);
        BigDecimal attachmentMultiplier = attachmentMultiplier(activity, profile, parameters);

        BigDecimal raw = parameters.get("score_base")
                .subtract(sensoryPenalty)
                .add(interestBonus)
                .add(periodBonus)
                .add(difficultyBonus)
                .multiply(attachmentMultiplier);
        BigDecimal roundedRaw = decimal(raw);
        BigDecimal display = roundedRaw.max(parameters.get("score_display_min"))
                .min(parameters.get("score_display_max"));

        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("base", parameters.get("score_base"));
        breakdown.put("D", sensoryPenalty);
        breakdown.put("G", interestBonus);
        breakdown.put("P", periodBonus);
        breakdown.put("Z", difficultyBonus);
        breakdown.put("B", attachmentMultiplier);
        breakdown.put("rawScore", roundedRaw);
        breakdown.put("displayScore", display);
        return new ScoredActivity(activity, roundedRaw, display, breakdown);
    }

    private BigDecimal sensoryPenalty(
            Activity activity,
            DunnProfile dunn,
            ChildSensoryAdjustment adjustment
    ) {
        BigDecimal noiseWeight = Objects.requireNonNull(dunn.getNoiseWeight(), "Dunn noise weight is required");
        BigDecimal visualWeight = Objects.requireNonNull(dunn.getVisualWeight(), "Dunn visual weight is required");
        BigDecimal movementWeight = Objects.requireNonNull(dunn.getMovementWeight(), "Dunn movement weight is required");
        short noiseTolerance = adjustedTolerance(
                dunn.getNoiseTolerance(), adjustment == null ? 0 : adjustment.getNoiseAdjustment());
        short visualTolerance = adjustedTolerance(
                dunn.getVisualTolerance(), adjustment == null ? 0 : adjustment.getVisualAdjustment());
        short movementTolerance = adjustedTolerance(
                dunn.getMovementTolerance(), adjustment == null ? 0 : adjustment.getMovementAdjustment());
        return noiseWeight.multiply(distance(noiseTolerance, activity.getNoiseLoad()))
                .add(visualWeight.multiply(distance(visualTolerance, activity.getVisualLoad())))
                .add(movementWeight.multiply(distance(movementTolerance, activity.getPhysicalIntensity())));
    }

    private short adjustedTolerance(short tolerance, short adjustment) {
        return (short) Math.max(1, Math.min(5, (int) tolerance + adjustment));
    }

    private BigDecimal interestBonus(
            Activity activity,
            Map<IntelligenceType, ChildIntelligenceScore> scores,
            Map<String, BigDecimal> parameters
    ) {
        BigDecimal target = scores.get(activity.getTargetIntelligence()).getScore();
        BigDecimal secondary = activity.getSecondaryIntelligence() == null
                ? null : scores.get(activity.getSecondaryIntelligence()).getScore();
        if (target.compareTo(parameters.get("gardner_comfort_threshold")) >= 0) {
            return parameters.get("gardner_comfort_bonus");
        }
        if (target.compareTo(parameters.get("gardner_bridge_target_threshold")) <= 0
                && secondary != null
                && secondary.compareTo(parameters.get("gardner_bridge_secondary_threshold")) >= 0) {
            return parameters.get("gardner_bridge_bonus");
        }
        if (target.compareTo(parameters.get("gardner_block_threshold")) <= 0) {
            return parameters.get("gardner_block_penalty");
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal difficultyBonus(
            Activity activity,
            Map<DevelopmentDomain, ChildDomainLevel> levels,
            Map<String, BigDecimal> parameters
    ) {
        int level = levels.get(activity.getTargetDomain()).getLevel();
        int difficulty = activity.getDifficulty();
        ActivityInstruction instruction = activity.getInstruction();
        boolean hasEasierVariation = hasText(instruction == null ? null : instruction.getEasierVariation());
        boolean hasHarderVariation = hasText(instruction == null ? null : instruction.getHarderVariation());
        int levelMax = parameters.get("level_max").intValueExact();
        boolean ceilingRequiresHarder = parameters.get("ceiling_sweet_spot_requires_harder").signum() != 0;

        if (difficulty == level + 1 && hasEasierVariation) {
            return parameters.get("zpd_sweet_spot_bonus");
        }
        if (level == levelMax && difficulty == level && (!ceilingRequiresHarder || hasHarderVariation)) {
            return parameters.get("zpd_sweet_spot_bonus");
        }
        if (difficulty == level) return BigDecimal.ZERO;
        if (difficulty < level) return parameters.get("zpd_boredom_penalty");
        if (difficulty > level + 1) return parameters.get("zpd_frustration_penalty");
        return BigDecimal.ZERO;
    }

    private BigDecimal attachmentMultiplier(
            Activity activity,
            ChildProfileSnapshot profile,
            Map<String, BigDecimal> parameters
    ) {
        boolean anxious = profile.getSeparationAnxiety() != null
                && profile.getSeparationAnxiety() >= parameters.get("attachment_anxiety_threshold").intValueExact();
        return anxious && activity.getInvolvementType() == InvolvementType.BIRLIKTE
                ? parameters.get("attachment_multiplier_together") : BigDecimal.ONE;
    }

    private BigDecimal distance(short tolerance, short load) {
        return BigDecimal.valueOf(Math.abs((int) tolerance - load));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private BigDecimal decimal(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
