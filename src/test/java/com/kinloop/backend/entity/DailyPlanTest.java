package com.kinloop.backend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinloop.backend.entity.enums.PlanSlotType;
import com.kinloop.backend.exception.ActivityNotInDailyPlanException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DailyPlanTest {

    @Test
    void newPlanAndItemsUseV6BookkeepingDefaults() {
        DailyPlan plan = new DailyPlan(10L, LocalDate.now());

        plan.add(activity(1L), PlanSlotType.DEVELOP, BigDecimal.ONE);
        plan.add(activity(2L), PlanSlotType.EXPLORE, BigDecimal.TEN, false, true);

        assertEquals(25, plan.getBudgetMin());
        assertEquals(35, plan.getBudgetMax());
        assertEquals(0, plan.getCommittedDurationMinutes());
        assertEquals(0, plan.getTotalDurationMinutes());
        assertEquals(0, plan.getFallbackLevel());
        assertTrue(plan.getItems().get(0).isWithinBudget());
        assertFalse(plan.getItems().get(0).isRepeatNotice());
        assertFalse(plan.getItems().get(1).isWithinBudget());
        assertTrue(plan.getItems().get(1).isRepeatNotice());
    }

    @Test
    void selectingAnotherActivityClearsThePreviousSelection() {
        DailyPlan plan = new DailyPlan(10L, LocalDate.now());
        Activity first = activity(1L);
        Activity second = activity(2L);
        plan.add(first, PlanSlotType.STRENGTHEN, BigDecimal.ONE);
        plan.add(second, PlanSlotType.DEVELOP, BigDecimal.TEN);

        plan.select(1L);
        assertTrue(plan.getItems().get(0).isSelected());
        assertFalse(plan.getItems().get(1).isSelected());

        plan.select(2L);
        assertFalse(plan.getItems().get(0).isSelected());
        assertTrue(plan.getItems().get(1).isSelected());
    }

    @Test
    void selectingAnActivityOutsideThePlanIsRejectedWithoutChangingSelection() {
        DailyPlan plan = new DailyPlan(10L, LocalDate.now());
        plan.add(activity(1L), PlanSlotType.EXPLORE, BigDecimal.ONE);
        plan.select(1L);

        assertThrows(ActivityNotInDailyPlanException.class, () -> plan.select(99L));
        assertTrue(plan.getItems().getFirst().isSelected());
    }

    private Activity activity(Long id) {
        Activity activity = new Activity();
        ReflectionTestUtils.setField(activity, "id", id);
        return activity;
    }
}
