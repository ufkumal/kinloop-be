package com.kinloop.backend.service.matching;

import static com.kinloop.backend.service.matching.MatchingTestFixtures.activity;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.InvolvementType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DailyPortfolioBuilderTest {
    private final DailyPortfolioBuilder builder = new DailyPortfolioBuilder();

    @Test
    void keepsRequiredDevelopmentActivityWhenBudgetOnlyFitsOneActivity() {
        ScoredActivity development = scored(301L, 10, "120");
        ScoredActivity strengthening = scored(302L, 10, "110");
        ScoredActivity exploration = scored(303L, 10, "100");

        List<Long> selectedActivityIds = builder.build(
                        List.of(development), List.of(strengthening), List.of(exploration), 10)
                .stream()
                .map(selection -> selection.activity().activity().getId())
                .toList();

        assertEquals(List.of(301L), selectedActivityIds);
    }

    private ScoredActivity scored(long id, int duration, String score) {
        var activity = activity(id, DevelopmentDomain.LANGUAGE, 2, (short) duration,
                3, 3, 2, InvolvementType.BIRLIKTE, true);
        BigDecimal value = new BigDecimal(score);
        return new ScoredActivity(activity, value, value.min(new BigDecimal("100")), Map.of());
    }
}
