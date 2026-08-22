package com.kinloop.backend.service.matching;

import static com.kinloop.backend.service.matching.MatchingTestFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.ChildProfileSnapshot;
import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.DunnQuadrant;
import com.kinloop.backend.entity.enums.FocusBand;
import com.kinloop.backend.entity.enums.InvolvementType;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ActivityEligibilityPolicyTest {
    private final ActivityEligibilityPolicy policy = new ActivityEligibilityPolicy();

    @Test
    void c4HardFilterUsesNoiseAndVisualButNotPhysicalIntensity() {
        ChildProfileSnapshot profile = new ChildProfileSnapshot();
        profile.setDunnQuadrant(DunnQuadrant.C4);
        profile.setSeparationAnxiety(2);

        var calm = activity(201L, DevelopmentDomain.SENSORY, 1, (short) 10,
                2, 2, 2, InvolvementType.BIRLIKTE, false);
        var physicallyActive = activity(202L, DevelopmentDomain.SENSORY, 1, (short) 10,
                2, 2, 5, InvolvementType.BIRLIKTE, false);
        var noisy = activity(203L, DevelopmentDomain.SENSORY, 1, (short) 10,
                3, 2, 2, InvolvementType.BIRLIKTE, false);

        List<Long> eligibleActivityIds = Stream.of(calm, physicallyActive, noisy)
                .filter(activity -> policy.allows(activity, profile, parameters()))
                .map(Activity::getId)
                .toList();

        assertEquals(List.of(201L, 202L), eligibleActivityIds);
    }

    @Test
    void anxiousChildAndShortFocusFiltersRemainActive() {
        ChildProfileSnapshot profile = new ChildProfileSnapshot();
        profile.setDunnQuadrant(DunnQuadrant.C1);
        profile.setSeparationAnxiety(4);
        profile.setFocusBand(FocusBand.SHORT);

        var togetherShort = activity(211L, DevelopmentDomain.LANGUAGE, 1, (short) 10,
                1, 1, 1, InvolvementType.BIRLIKTE, false);
        var independentShort = activity(212L, DevelopmentDomain.LANGUAGE, 1, (short) 10,
                1, 1, 1, InvolvementType.BAGIMSIZ, false);
        var togetherLong = activity(213L, DevelopmentDomain.LANGUAGE, 1, (short) 15,
                1, 1, 1, InvolvementType.BIRLIKTE, false);

        List<Long> eligibleActivityIds = Stream.of(togetherShort, independentShort, togetherLong)
                .filter(activity -> policy.allows(activity, profile, parameters()))
                .map(Activity::getId)
                .toList();

        assertEquals(List.of(211L), eligibleActivityIds);
    }
}
