package com.kinloop.backend.repository;

import com.kinloop.backend.entity.Child;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface ChildRepository extends JpaRepository<Child, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Child c where c.id = :id")
    Optional<Child> findLockedById(Long id);

    List<Child> findByParentIdAndDeletedAtIsNullOrderByIdAsc(Long parentId);

    long countByParentIdAndFullNameIsNullAndDeletedAtIsNull(Long parentId);

    Optional<Child> findByIdAndParentIdAndDeletedAtIsNull(Long id, Long parentId);

    boolean existsByParentIdAndOnboardingCompletedAtIsNotNullAndDeletedAtIsNull(Long parentId);
}
