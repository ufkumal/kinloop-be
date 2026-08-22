package com.kinloop.backend.repository;

import com.kinloop.backend.entity.DailyPlan;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DailyPlanRepository extends JpaRepository<DailyPlan, Long> {
    @EntityGraph(attributePaths = {"items", "items.activity", "items.activity.instruction"})
    Optional<DailyPlan> findByChildIdAndPlanDate(Long childId, LocalDate date);

    @Query(value = """
            SELECT DISTINCT dpi.activity_id
            FROM daily_plan_items dpi
            JOIN (
                SELECT id
                FROM daily_plans
                WHERE child_id = :childId AND plan_date < :beforeDate
                ORDER BY plan_date DESC
                LIMIT :planLimit
            ) recent_plans ON recent_plans.id = dpi.daily_plan_id
            """, nativeQuery = true)
    java.util.Set<Long> findActivityIdsInRecentPlans(
            @Param("childId") Long childId,
            @Param("beforeDate") LocalDate beforeDate,
            @Param("planLimit") int planLimit
    );
}
