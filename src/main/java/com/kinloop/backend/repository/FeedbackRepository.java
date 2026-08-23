package com.kinloop.backend.repository;

import com.kinloop.backend.entity.Feedback;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    boolean existsByDailyPlanItemId(Long dailyPlanItemId);

    Optional<Feedback> findByChildIdAndDailyPlanItemId(Long childId, Long dailyPlanItemId);
}
