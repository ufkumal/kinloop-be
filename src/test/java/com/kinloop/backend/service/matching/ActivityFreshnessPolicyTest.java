package com.kinloop.backend.service.matching;

import static com.kinloop.backend.service.matching.MatchingTestFixtures.parameters;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.InvolvementType;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class ActivityFreshnessPolicyTest {
    private final ActivityFreshnessPolicy policy = new ActivityFreshnessPolicy();

    @Test
    void windowIsMaximumOfTwoAndCeilingOfPoolSizeDividedBySix() {
        assertEquals(2, policy.windowSize(6, parameters()));
        assertEquals(3, policy.windowSize(17, parameters()));
        assertEquals(5, policy.windowSize(30, parameters()));
        assertEquals(7, policy.windowSize(41, parameters()));
    }

    @Test
    void eliminatesRecentActivitiesAndRetainsThePreFreshnessPool() {
        List<Activity> pool = LongStream.rangeClosed(1, 4).mapToObj(this::activity).toList();

        ActivityFreshnessPolicy.Result result = policy.eliminate(pool, Set.of(2L, 4L), 2);

        assertEquals(List.of(1L, 2L, 3L, 4L), ids(result.preFreshnessPool()));
        assertEquals(List.of(1L, 3L), ids(result.eligiblePool()));
        assertEquals(Set.of(2L, 4L), result.excludedActivityIds());
        assertEquals(2, result.windowPlans());
    }

    private Activity activity(long id) {
        return MatchingTestFixtures.activity(id, DevelopmentDomain.LANGUAGE, 1, (short) 10,
                1, 1, 1, InvolvementType.BIRLIKTE, true);
    }

    private List<Long> ids(List<Activity> activities) {
        return activities.stream().map(Activity::getId).toList();
    }
}
