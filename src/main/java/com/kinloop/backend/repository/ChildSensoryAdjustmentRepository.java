package com.kinloop.backend.repository;

import com.kinloop.backend.entity.ChildSensoryAdjustment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildSensoryAdjustmentRepository extends JpaRepository<ChildSensoryAdjustment, Long> {
    Optional<ChildSensoryAdjustment> findByChildId(Long childId);
}
