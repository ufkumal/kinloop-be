package com.kinloop.backend.repository;

import com.kinloop.backend.entity.Child;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildRepository extends JpaRepository<Child, Long> {

    List<Child> findByParentIdAndDeletedAtIsNullOrderByIdAsc(Long parentId);

    long countByParentIdAndFullNameIsNullAndDeletedAtIsNull(Long parentId);
}
