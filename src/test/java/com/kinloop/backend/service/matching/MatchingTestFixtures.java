package com.kinloop.backend.service.matching;

import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.ActivityInstruction;
import com.kinloop.backend.entity.ChildDomainLevel;
import com.kinloop.backend.entity.ChildIntelligenceScore;
import com.kinloop.backend.entity.DunnProfile;
import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.DunnQuadrant;
import com.kinloop.backend.entity.enums.IntelligenceType;
import com.kinloop.backend.entity.enums.InvolvementType;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.test.util.ReflectionTestUtils;

final class MatchingTestFixtures {
    private MatchingTestFixtures() {
    }

    static Activity activity(long id, DevelopmentDomain domain, int difficulty, short duration,
                             int noise, int visual, int movement, InvolvementType involvement, boolean supported) {
        Activity activity = new Activity();
        set(activity, "id", id);
        set(activity, "targetDomain", domain);
        set(activity, "targetIntelligence", IntelligenceType.VERBAL_LINGUISTIC);
        set(activity, "difficulty", (short) difficulty);
        set(activity, "durationMinutes", duration);
        set(activity, "noiseLoad", (short) noise);
        set(activity, "visualLoad", (short) visual);
        set(activity, "physicalIntensity", (short) movement);
        set(activity, "involvementType", involvement);
        if (supported) {
            ActivityInstruction instruction = new ActivityInstruction();
            set(instruction, "easierVariation", "İki adımla başlayın");
            set(activity, "instruction", instruction);
        }
        return activity;
    }

    static DunnProfile c1Profile() {
        DunnProfile profile = new DunnProfile();
        set(profile, "quadrant", DunnQuadrant.C1);
        set(profile, "noiseTolerance", (short) 3);
        set(profile, "visualTolerance", (short) 3);
        set(profile, "movementTolerance", (short) 3);
        set(profile, "noiseWeight", new BigDecimal("5"));
        set(profile, "visualWeight", new BigDecimal("5"));
        set(profile, "movementWeight", new BigDecimal("3"));
        return profile;
    }

    static Map<IntelligenceType, ChildIntelligenceScore> neutralIntelligenceScores() {
        Map<IntelligenceType, ChildIntelligenceScore> scores = new EnumMap<>(IntelligenceType.class);
        for (IntelligenceType type : IntelligenceType.values()) {
            scores.put(type, new ChildIntelligenceScore(1L, type, new BigDecimal("3.00")));
        }
        return scores;
    }

    static Map<DevelopmentDomain, ChildDomainLevel> levelOneDomains() {
        Map<DevelopmentDomain, ChildDomainLevel> levels = new EnumMap<>(DevelopmentDomain.class);
        for (DevelopmentDomain domain : DevelopmentDomain.values()) {
            levels.put(domain, new ChildDomainLevel(1L, domain, (short) 1));
        }
        return levels;
    }

    static Map<String, BigDecimal> parameters() {
        Map<String, BigDecimal> values = new java.util.HashMap<>();
        values.put("short_focus_max_duration_minutes", bd("10"));
        values.put("attachment_anxiety_threshold", bd("4"));
        values.put("c4_hard_filter_load_threshold", bd("3"));
        values.put("score_base", bd("100"));
        values.put("score_display_min", bd("0"));
        values.put("score_display_max", bd("100"));
        values.put("developmental_period_bonus", bd("15"));
        values.put("zpd_sweet_spot_bonus", bd("20"));
        values.put("zpd_frustration_penalty", bd("-25"));
        values.put("zpd_boredom_penalty", bd("-5"));
        values.put("gardner_comfort_threshold", bd("4"));
        values.put("gardner_comfort_bonus", bd("10"));
        values.put("gardner_bridge_target_threshold", bd("2.5"));
        values.put("gardner_bridge_secondary_threshold", bd("4"));
        values.put("gardner_bridge_bonus", bd("15"));
        values.put("gardner_block_threshold", bd("1.5"));
        values.put("gardner_block_penalty", bd("-15"));
        values.put("attachment_multiplier_together", bd("1.15"));
        values.put("level_max", bd("4"));
        values.put("ceiling_sweet_spot_requires_harder", bd("1"));
        values.put("level_min", bd("1"));
        values.put("level_up_threshold", bd("3.0"));
        values.put("level_down_threshold", bd("-1.0"));
        values.put("level_credit_stretch", bd("1.0"));
        values.put("level_credit_at_level", bd("0.5"));
        values.put("level_credit_below", bd("0.0"));
        values.put("level_penalty_struggle_stretch", bd("-0.5"));
        values.put("level_penalty_struggle_at_level", bd("-1.0"));
        values.put("ceiling_counter_cap", bd("1.0"));
        values.put("domain_initial_level_under_48m", bd("1"));
        values.put("domain_initial_level_48_to_60m", bd("2"));
        values.put("domain_initial_level_60_to_73m", bd("3"));
        values.put("liked_target_delta", bd("0.30"));
        values.put("liked_secondary_delta", bd("0.15"));
        values.put("disliked_interest_delta", bd("-0.15"));
        values.put("gardner_runtime_min_score", bd("0.00"));
        values.put("gardner_runtime_max_score", bd("5.00"));
        values.put("attachment_guarantee_supervised", bd("1"));
        values.put("freshness_window_divisor", bd("6"));
        values.put("freshness_window_min", bd("2"));
        values.put("tiebreak_seed_a", bd("1000003"));
        values.put("tiebreak_seed_b", bd("10007"));
        values.put("tiebreak_seed_mod", bd("2147483647"));
        return values;
    }

    static void set(Object target, String field, Object value) {
        ReflectionTestUtils.setField(target, field, value);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
