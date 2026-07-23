package com.kinloop.backend.repository;

import com.kinloop.backend.entity.WorkshopProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkshopProfileRepository extends JpaRepository<WorkshopProfile, Long> {

    Optional<WorkshopProfile> findByUserIdAndDeletedAtIsNull(Long userId);
}
