package com.kinloop.backend.repository;

import com.kinloop.backend.entity.Child;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildRepository extends JpaRepository<Child, Long> {

    Optional<Child> findByIdAndDeletedAtIsNull(Long id);

    List<Child> findByParentIdAndDeletedAtIsNullOrderByIdAsc(Long parentId);

    long countByParentIdAndFullNameIsNullAndDeletedAtIsNull(Long parentId);
}
