package com.kinloop.backend.repository;

import com.kinloop.backend.entity.FeedbackLlmClassification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackLlmClassificationRepository
        extends JpaRepository<FeedbackLlmClassification, Long> {
}
