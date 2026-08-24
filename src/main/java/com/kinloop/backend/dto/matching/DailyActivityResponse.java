package com.kinloop.backend.dto.matching;

import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.IntelligenceType;
import com.kinloop.backend.entity.enums.InvolvementType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record DailyActivityResponse(
        Long dailyPlanItemId,
        Long activityId,
        String title,
        String description,
        int minAgeMonths,
        int maxAgeMonths,
        IntelligenceType targetIntelligence,
        IntelligenceType secondaryIntelligence,
        DevelopmentDomain targetDomain,
        short difficulty,
        short durationMinutes,
        InvolvementType involvementType,
        short noiseLoad,
        short visualLoad,
        short physicalIntensity,
        String slotType,
        BigDecimal score,
        String intro,
        String purpose,
        String whyItMatters,
        String easierVariation,
        String harderVariation,
        String observationTip,
        String safetyNotes,
        String cleanupNotes,
        List<ActivityStepResponse> steps,
        List<ActivityMaterialResponse> materials,
        List<ActivityOutcomeResponse> outcomes,
        boolean withinBudget,
        boolean repeatNotice,
        boolean selected,
        boolean completed,
        OffsetDateTime selectedAt,
        OffsetDateTime completedAt
) {
}
