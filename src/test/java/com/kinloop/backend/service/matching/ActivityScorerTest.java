package com.kinloop.backend.service.matching;

import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.ChildProfileSnapshot;
import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.DunnQuadrant;
import com.kinloop.backend.entity.enums.InvolvementType;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.stream.Stream;

import static com.kinloop.backend.service.matching.MatchingTestFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ActivityScorerTest {
    private final ActivityScorer scorer = new ActivityScorer();

    @Test
    void selectsLanguageActivityInTheChildsZpdSweetSpot() {
        ChildProfileSnapshot profile = new ChildProfileSnapshot();
        profile.setDunnQuadrant(DunnQuadrant.C1);
        profile.setSeparationAnxiety(2);

        Activity sweetSpot = activity(101L, DevelopmentDomain.LANGUAGE, 2, (short) 10,
                3, 3, 2, InvolvementType.BIRLIKTE, true);
        Activity frustrating = activity(102L, DevelopmentDomain.LANGUAGE, 3, (short) 10,
                3, 3, 2, InvolvementType.BIRLIKTE, true);

        Long selectedActivityId = Stream.of(sweetSpot, frustrating)
                .map(activity -> scorer.score(activity, profile, c1Profile(), DevelopmentDomain.LANGUAGE,
                        neutralIntelligenceScores(), levelOneDomains(), parameters()))
                .max(Comparator.comparing(ScoredActivity::rawScore))
                .orElseThrow()
                .activity()
                .getId();

        assertEquals(101L, selectedActivityId);
    }

    @Test
    void levelFourDifficultyFourRequiresHarderVariationForSweetSpot() {
        ChildProfileSnapshot profile = profile(2);
        Activity withHarderVariation = activity(111L, DevelopmentDomain.LANGUAGE, 4, (short) 10,
                3, 3, 3, InvolvementType.BIRLIKTE, true);
        set(withHarderVariation.getInstruction(), "harderVariation", "Add another rule");
        Activity withoutHarderVariation = activity(112L, DevelopmentDomain.LANGUAGE, 4, (short) 10,
                3, 3, 3, InvolvementType.BIRLIKTE, true);
        var levels = levelOneDomains();
        levels.put(DevelopmentDomain.LANGUAGE,
                new com.kinloop.backend.entity.ChildDomainLevel(1L, DevelopmentDomain.LANGUAGE, (short) 4));

        ScoredActivity sweetSpot = scorer.score(withHarderVariation, profile, c1Profile(),
                DevelopmentDomain.SOCIAL_EMOTIONAL, neutralIntelligenceScores(), levels, parameters());
        ScoredActivity atLevel = scorer.score(withoutHarderVariation, profile, c1Profile(),
                DevelopmentDomain.SOCIAL_EMOTIONAL, neutralIntelligenceScores(), levels, parameters());

        assertEquals(new java.math.BigDecimal("20"), sweetSpot.breakdown().get("Z"));
        assertEquals(java.math.BigDecimal.ZERO, atLevel.breakdown().get("Z"));
    }

    @Test
    void attachmentMultiplierAppliesOnlyToAnxiousTogetherActivities() {
        ChildProfileSnapshot profile = profile(4);
        Activity together = activity(121L, DevelopmentDomain.LANGUAGE, 1, (short) 10,
                3, 3, 3, InvolvementType.BIRLIKTE, true);
        Activity supervised = activity(122L, DevelopmentDomain.LANGUAGE, 1, (short) 10,
                3, 3, 3, InvolvementType.GOZETIMLI, true);

        ScoredActivity togetherScore = scorer.score(together, profile, c1Profile(), DevelopmentDomain.LANGUAGE,
                neutralIntelligenceScores(), levelOneDomains(), parameters());
        ScoredActivity supervisedScore = scorer.score(supervised, profile, c1Profile(), DevelopmentDomain.LANGUAGE,
                neutralIntelligenceScores(), levelOneDomains(), parameters());

        assertEquals(new java.math.BigDecimal("1.15"), togetherScore.breakdown().get("B"));
        assertEquals(java.math.BigDecimal.ONE, supervisedScore.breakdown().get("B"));
        assertFalse(togetherScore.breakdown().containsKey("T"));
    }

    private ChildProfileSnapshot profile(int anxiety) {
        ChildProfileSnapshot profile = new ChildProfileSnapshot();
        profile.setDunnQuadrant(DunnQuadrant.C1);
        profile.setSeparationAnxiety(anxiety);
        return profile;
    }
}
