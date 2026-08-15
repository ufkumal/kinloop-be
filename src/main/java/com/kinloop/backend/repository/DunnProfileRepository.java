package com.kinloop.backend.repository;

import com.kinloop.backend.entity.DunnProfile;
import com.kinloop.backend.entity.enums.DunnQuadrant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DunnProfileRepository extends JpaRepository<DunnProfile, DunnQuadrant> {
}
