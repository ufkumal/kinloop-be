package com.kinloop.backend.service.matching;

import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.ChildProfileSnapshot;
import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.DunnQuadrant;
import com.kinloop.backend.entity.enums.InvolvementType;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

import static com.kinloop.backend.service.matching.MatchingTestFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
                        neutralIntelligenceScores(), levelOneDomains(), Set.of(), parameters()))
                .max(Comparator.comparing(ScoredActivity::rawScore))
                .orElseThrow()
                .activity()
                .getId();

        assertEquals(101L, selectedActivityId);
    }
}
