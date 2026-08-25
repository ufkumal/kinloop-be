package com.kinloop.backend.entity;

import com.kinloop.backend.entity.enums.PlanSlotType;
import com.kinloop.backend.exception.ActivityNotInDailyPlanException;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "daily_plans")
public class DailyPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "child_id", nullable = false)
    private Long childId;
    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;
    @Column(name = "budget_min", nullable = false)
    private int budgetMin = 25;
    @Column(name = "budget_max", nullable = false)
    private int budgetMax = 35;
    @Column(name = "committed_duration_minutes", nullable = false)
    private int committedDurationMinutes;
    @Column(name = "total_duration_minutes", nullable = false)
    private int totalDurationMinutes;
    @Column(name = "fallback_level", nullable = false)
    private short fallbackLevel;
    @OneToMany(mappedBy = "dailyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DailyPlanItem> items = new ArrayList<>();

    public DailyPlan(Long childId, LocalDate date) {
        this(childId, date, (short) 25, (short) 35);
    }

    public DailyPlan(Long childId, LocalDate date, short budgetMin, short budgetMax) {
        this.childId = childId;
        this.planDate = date;
        this.budgetMin = budgetMin;
        this.budgetMax = budgetMax;
    }

    public void add(Activity activity, PlanSlotType slot, java.math.BigDecimal score) {
        add(activity, slot, score, true, false);
    }

    public void add(
            Activity activity,
            PlanSlotType slot,
            java.math.BigDecimal score,
            boolean withinBudget,
            boolean repeatNotice
    ) {
        if (items.stream().anyMatch(item -> item.getSlotType() == slot)) {
            throw new IllegalArgumentException("Daily plan slot already filled: " + slot);
        }
        if (items.stream().anyMatch(item -> item.getActivity().getId().equals(activity.getId()))) {
            throw new IllegalArgumentException("Activity already exists in daily plan: " + activity.getId());
        }
        items.add(new DailyPlanItem(this, activity, slot, score, withinBudget, repeatNotice));
    }

    public void recordBookkeeping(
            int committedDurationMinutes,
            int totalDurationMinutes,
            short fallbackLevel
    ) {
        if (committedDurationMinutes < 0 || committedDurationMinutes > budgetMax) {
            throw new IllegalArgumentException("Committed duration must be between zero and budget maximum");
        }
        if (totalDurationMinutes < committedDurationMinutes) {
            throw new IllegalArgumentException("Total duration cannot be less than committed duration");
        }
        if (fallbackLevel < 0 || fallbackLevel > 4) {
            throw new IllegalArgumentException("Fallback level must be between 0 and 4");
        }
        this.committedDurationMinutes = committedDurationMinutes;
        this.totalDurationMinutes = totalDurationMinutes;
        this.fallbackLevel = fallbackLevel;
    }

    /**
     * A non-empty plan can be replaced only after feedback submission has
     * completed every item in the current three-activity round.
     */
    public boolean isRoundCompleted() {
        return !items.isEmpty() && items.stream().distinct().allMatch(DailyPlanItem::isCompleted);
    }

    public void select(Long activityId) {
        DailyPlanItem selected = item(activityId);
        prepareSelection(activityId);
        selected.select();
    }

    /**
     * Clears an unfinished selection before activating another item. Keeping
     * this as a separate step lets persistence flush the old active row before
     * setting the new one, which is required by the partial unique index.
     */
    public void prepareSelection(Long activityId) {
        DailyPlanItem selected = item(activityId);
        items.stream()
                .filter(item -> item != selected && item.isSelected())
                .forEach(DailyPlanItem::unselect);
    }

    private DailyPlanItem item(Long activityId) {
        return items.stream()
                .filter(item -> item.getActivity().getId().equals(activityId))
                .findFirst()
                .orElseThrow(() -> new ActivityNotInDailyPlanException(activityId));
    }
}
