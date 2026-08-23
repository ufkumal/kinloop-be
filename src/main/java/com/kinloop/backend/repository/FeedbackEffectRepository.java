package com.kinloop.backend.repository;

import com.kinloop.backend.entity.FeedbackEffect;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackEffectRepository extends JpaRepository<FeedbackEffect, Long> {
    List<FeedbackEffect> findByFeedbackIdAndReversedAtIsNull(Long feedbackId);
}
