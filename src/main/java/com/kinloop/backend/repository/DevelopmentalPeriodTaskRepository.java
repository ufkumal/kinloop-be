package com.kinloop.backend.repository;

import com.kinloop.backend.entity.DevelopmentalPeriodTask;

import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DevelopmentalPeriodTaskRepository extends JpaRepository<DevelopmentalPeriodTask, Long> {
    @Query("select p from DevelopmentalPeriodTask p where p.minAgeMonths<=:age and p.maxAgeMonths>:age")
    Optional<DevelopmentalPeriodTask> findForAge(@Param("age") int age);
}
