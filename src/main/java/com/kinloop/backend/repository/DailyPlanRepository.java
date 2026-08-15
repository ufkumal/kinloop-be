package com.kinloop.backend.repository;

import com.kinloop.backend.entity.DailyPlan;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DailyPlanRepository extends JpaRepository<DailyPlan, Long> {
    @EntityGraph(attributePaths = {"items", "items.activity", "items.activity.instruction"})
    Optional<DailyPlan> findByChildIdAndPlanDate(Long childId, LocalDate date);

    @Query("select i.activity.id from DailyPlanItem i where i.dailyPlan.childId=:childId and i.dailyPlan.planDate=:date")
    java.util.Set<Long> findActivityIds(@Param("childId") Long childId, @Param("date") LocalDate date);
}
