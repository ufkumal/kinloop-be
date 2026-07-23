package com.kinloop.backend.repository;

import com.kinloop.backend.entity.ParentProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentProfileRepository extends JpaRepository<ParentProfile, Long> {

    Optional<ParentProfile> findByUserIdAndDeletedAtIsNull(Long userId);
}
