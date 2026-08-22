package com.kinloop.backend.service.matching;

import com.kinloop.backend.entity.Activity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ActivityFreshnessPolicy {

    public int windowSize(int poolSize, Map<String, BigDecimal> parameters) {
        int divisor = parameters.get("freshness_window_divisor").intValueExact();
        int minimum = parameters.get("freshness_window_min").intValueExact();
        if (divisor <= 0) throw new IllegalArgumentException("freshness_window_divisor must be positive");
        int quotient = poolSize / divisor;
        int ceiling = quotient + (poolSize % divisor == 0 ? 0 : 1);
        return Math.max(minimum, ceiling);
    }

    public Result eliminate(List<Activity> preFreshnessPool, Set<Long> recentActivityIds, int windowPlans) {
        List<Activity> eligible = preFreshnessPool.stream()
                .filter(activity -> !recentActivityIds.contains(activity.getId()))
                .toList();
        return new Result(List.copyOf(preFreshnessPool), eligible, Set.copyOf(recentActivityIds), windowPlans);
    }

    /**
     * The complete pre-freshness pool is retained so fallback level 1 can
     * reintroduce recently used activities and mark them with repeat_notice.
     */
    public record Result(
            List<Activity> preFreshnessPool,
            List<Activity> eligiblePool,
            Set<Long> excludedActivityIds,
            int windowPlans
    ) {
    }
}
