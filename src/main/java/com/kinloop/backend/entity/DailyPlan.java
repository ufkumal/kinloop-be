package com.kinloop.backend.entity;

import com.kinloop.backend.entity.enums.PlanSlotType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "daily_plans", uniqueConstraints = @UniqueConstraint(columnNames = {"child_id", "plan_date"}))
public class DailyPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "child_id", nullable = false)
    private Long childId;
    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;
    @OneToMany(mappedBy = "dailyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DailyPlanItem> items = new ArrayList<>();

    public DailyPlan(Long childId, LocalDate date) {
        this.childId = childId;
        this.planDate = date;
    }

    public void add(Activity activity, PlanSlotType slot, java.math.BigDecimal score) {
        items.add(new DailyPlanItem(this, activity, slot, score));
    }
}
