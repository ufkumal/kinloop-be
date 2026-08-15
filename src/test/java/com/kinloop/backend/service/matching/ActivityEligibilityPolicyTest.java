package com.kinloop.backend.service.matching;

import static com.kinloop.backend.service.matching.MatchingTestFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.ChildProfileSnapshot;
import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.DunnQuadrant;
import com.kinloop.backend.entity.enums.InvolvementType;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ActivityEligibilityPolicyTest {
    private final ActivityEligibilityPolicy policy = new ActivityEligibilityPolicy();

    @Test
    void c4ProfileKeepsOnlyActivityBelowEverySensoryThreshold() {
        ChildProfileSnapshot profile = new ChildProfileSnapshot();
        profile.setDunnQuadrant(DunnQuadrant.C4);
        profile.setSeparationAnxiety(2);

        var calm = activity(201L, DevelopmentDomain.SENSORY, 1, (short) 10,
                2, 2, 2, InvolvementType.BIRLIKTE, false);
        var noisy = activity(202L, DevelopmentDomain.SENSORY, 1, (short) 10,
                3, 2, 2, InvolvementType.BIRLIKTE, false);

        List<Long> eligibleActivityIds = Stream.of(calm, noisy)
                .filter(activity -> policy.allows(activity, profile, parameters()))
                .map(Activity::getId)
                .toList();

        assertEquals(List.of(201L), eligibleActivityIds);
    }
}
