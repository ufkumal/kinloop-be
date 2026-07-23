package com.kinloop.backend.repository;
import com.kinloop.backend.entity.ChildProfileSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ChildProfileSnapshotRepository extends JpaRepository<ChildProfileSnapshot, Long> {
    Optional<ChildProfileSnapshot> findByChildIdAndCurrentTrue(Long childId);
}
