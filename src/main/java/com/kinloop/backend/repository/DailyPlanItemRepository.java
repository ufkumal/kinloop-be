package com.kinloop.backend.repository;

import com.kinloop.backend.entity.DailyPlanItem;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyPlanItemRepository extends JpaRepository<DailyPlanItem, Long> {

    @EntityGraph(attributePaths = {"dailyPlan", "activity"})
    @Query("""
            select item
            from DailyPlanItem item
            where item.selectedAt is not null
              and item.dailyPlan.childId in (
                  select child.id
                  from Child child
                  where child.parentId = :parentId
                    and child.deletedAt is null
              )
            order by item.selectedAt desc, item.id desc
            """)
    List<DailyPlanItem> findLatestSelectedByParentId(@Param("parentId") Long parentId, Pageable pageable);

    @EntityGraph(attributePaths = {"dailyPlan", "activity"})
    @Query("""
            select item
            from DailyPlanItem item
            where item.dailyPlan.childId = :childId
              and item.selectedAt is not null
            order by item.selectedAt desc, item.id desc
            """)
    Slice<DailyPlanItem> findSelectedHistoryByChildId(@Param("childId") Long childId, Pageable pageable);
}
