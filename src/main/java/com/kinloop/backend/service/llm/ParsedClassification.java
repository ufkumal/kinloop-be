package com.kinloop.backend.service.llm;

import com.kinloop.backend.entity.enums.DifficultyHint;
import com.kinloop.backend.entity.enums.DurationHint;
import com.kinloop.backend.entity.enums.IntelligenceType;
import com.kinloop.backend.entity.enums.InvolvementHint;
import com.kinloop.backend.entity.enums.SensoryHint;
import com.kinloop.backend.entity.enums.SituationHint;
import java.math.BigDecimal;

/**
 * Validated result of parsing one model response (Kidloop_FewShot_Prompt_v2.md §10).
 * valid=false means the response was unusable (unparseable JSON, or confidence missing
 * or outside 0.0-1.0) — nothing in it should be trusted, including confidence itself.
 * When valid=true, individual fields may still be null: either the model said so, or an
 * unrecognized enum value was coerced to null rather than rejecting the whole response.
 */
public record ParsedClassification(
        boolean valid,
        BigDecimal confidence,
        IntelligenceType targetCorrection,
        IntelligenceType secondaryHint,
        SensoryHint sensoryHint,
        InvolvementHint involvementHint,
        DifficultyHint difficultyHint,
        SituationHint situationHint,
        DurationHint durationHint,
        boolean conflict
) {
    public static ParsedClassification invalid() {
        return new ParsedClassification(
                false, null, null, null, null, null, null, null, null, false);
    }
}
