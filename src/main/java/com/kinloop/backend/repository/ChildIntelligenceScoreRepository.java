package com.kinloop.backend.repository;

import com.kinloop.backend.entity.ChildIntelligenceScore;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildIntelligenceScoreRepository extends JpaRepository<ChildIntelligenceScore, Long> {
    List<ChildIntelligenceScore> findByChildId(Long childId);

    boolean existsByChildId(Long childId);
}
