package com.kinloop.backend.entity;

import com.kinloop.backend.entity.enums.PlanSlotType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "daily_plan_items")
public class DailyPlanItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_plan_id")
    private DailyPlan dailyPlan;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id")
    private Activity activity;
    @Enumerated(EnumType.STRING)
    @Column(name = "slot_type")
    private PlanSlotType slotType;
    @Column
    private BigDecimal score;
    @Column(nullable = false)
    private String source = "RULE";
    @Column(name = "selected_at")
    private OffsetDateTime selectedAt;
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    DailyPlanItem(DailyPlan plan, Activity activity, PlanSlotType slot, BigDecimal score) {
        this.dailyPlan = plan;
        this.activity = activity;
        this.slotType = slot;
        this.score = score;
    }

    public boolean isSelected() {
        return selectedAt != null;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    void select() {
        selectedAt = OffsetDateTime.now();
    }

    void unselect() {
        selectedAt = null;
    }
}
