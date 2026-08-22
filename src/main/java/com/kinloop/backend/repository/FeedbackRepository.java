package com.kinloop.backend.repository;

import com.kinloop.backend.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    boolean existsByDailyPlanItemId(Long dailyPlanItemId);
}
