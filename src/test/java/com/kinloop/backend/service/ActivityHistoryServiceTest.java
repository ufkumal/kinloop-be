package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.kinloop.backend.dto.profile.ActivityHistoryResponse;
import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.DailyPlan;
import com.kinloop.backend.entity.DailyPlanItem;
import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.InvolvementType;
import com.kinloop.backend.entity.enums.PlanSlotType;
import com.kinloop.backend.mapper.ProfileMapper;
import com.kinloop.backend.repository.DailyPlanItemRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ActivityHistoryServiceTest {

    @Mock private DailyPlanItemRepository dailyPlanItemRepository;

    @Test
    void selectedHistoryExposesCompletionAsASeparateState() {
        DailyPlanItem item = historyItem();
        when(dailyPlanItemRepository.findSelectedHistoryByChildId(7L, PageRequest.of(0, 20)))
                .thenReturn(new SliceImpl<>(java.util.List.of(item), PageRequest.of(0, 20), false));
        ActivityHistoryService service = new ActivityHistoryService(
                dailyPlanItemRepository, new ProfileMapper(java.util.List.of()));

        ActivityHistoryResponse response = service.getSelectedHistory(7L, 0, 20);

        assertFalse(response.hasNext());
        assertEquals(1, response.activities().size());
        assertTrue(response.activities().getFirst().completed());
        assertEquals(LocalDate.of(2026, 8, 4), response.activities().getFirst().planDate());
        assertEquals("Evde renk avı", response.activities().getFirst().title());
    }

    private DailyPlanItem historyItem() {
        Activity activity = new Activity();
        ReflectionTestUtils.setField(activity, "id", 12L);
        ReflectionTestUtils.setField(activity, "title", "Evde renk avı");
        ReflectionTestUtils.setField(activity, "description", "Üç rengi birlikte arayın.");
        ReflectionTestUtils.setField(activity, "durationMinutes", (short) 15);
        ReflectionTestUtils.setField(activity, "targetDomain", DevelopmentDomain.COGNITIVE);
        ReflectionTestUtils.setField(activity, "involvementType", InvolvementType.BIRLIKTE);
        DailyPlan plan = new DailyPlan(7L, LocalDate.of(2026, 8, 4));
        plan.add(activity, PlanSlotType.DEVELOP, BigDecimal.TEN);
        plan.select(12L);
        DailyPlanItem item = plan.getItems().getFirst();
        ReflectionTestUtils.setField(item, "id", 30L);
        ReflectionTestUtils.setField(item, "completedAt", OffsetDateTime.now());
        return item;
    }
}
