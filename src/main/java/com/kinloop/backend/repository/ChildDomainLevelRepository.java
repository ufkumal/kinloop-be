package com.kinloop.backend.repository;

import com.kinloop.backend.entity.ChildDomainLevel;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildDomainLevelRepository extends JpaRepository<ChildDomainLevel, Long> {
    List<ChildDomainLevel> findByChildId(Long childId);
}
