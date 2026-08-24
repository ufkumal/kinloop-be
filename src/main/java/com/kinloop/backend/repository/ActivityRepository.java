package com.kinloop.backend.repository;

import com.kinloop.backend.entity.Activity;

import java.util.List;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    @EntityGraph(attributePaths = {"instruction", "steps", "materials", "outcomes"})
    @Query("select a from Activity a where a.scope='HOME' and a.status='PUBLISHED' " +
            "and a.deletedAt is null and a.minAgeMonths<=:age and a.maxAgeMonths>=:age and a.durationMinutes<=:budget")
    List<Activity> findEligibleBasePool(@Param("age") int age, @Param("budget") short budget);
}
