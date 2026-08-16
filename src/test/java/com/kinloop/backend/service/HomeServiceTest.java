package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinloop.backend.dto.home.HomeStatusResponse;
import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.DailyPlan;
import com.kinloop.backend.entity.DailyPlanItem;
import com.kinloop.backend.entity.enums.PlanSlotType;
import com.kinloop.backend.repository.ChildRepository;
import com.kinloop.backend.repository.DailyPlanItemRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock private ChildRepository childRepository;
    @Mock private DailyPlanItemRepository dailyPlanItemRepository;
    @Mock private ChildService childService;

    private HomeService service;

    @BeforeEach
    void setUp() {
        service = new HomeService(childRepository, dailyPlanItemRepository, childService);
    }

    @Test
    void parentWithoutChildReceivesNewUser() {
        when(childRepository.existsByParentIdAndOnboardingCompletedAtIsNotNullAndDeletedAtIsNull(1L))
                .thenReturn(false);

        HomeStatusResponse response = service.getStatus(1L);

        assertEquals("new-user", response.state());
    }

    @Test
    void parentWithIncompleteOnboardingReceivesNewUser() {
        when(childRepository.existsByParentIdAndOnboardingCompletedAtIsNotNullAndDeletedAtIsNull(1L))
                .thenReturn(false);

        HomeStatusResponse response = service.getStatus(1L);

        assertEquals("new-user", response.state());
    }

    @Test
    void parentWithCompletedOnboardingReceivesReturningUser() {
        when(childRepository.existsByParentIdAndOnboardingCompletedAtIsNotNullAndDeletedAtIsNull(1L))
                .thenReturn(true);

        HomeStatusResponse response = service.getStatus(1L);

        assertEquals("returning-user", response.state());
    }

    @Test
    void statusIsScopedToAuthenticatedParentProfile() {
        when(childRepository.existsByParentIdAndOnboardingCompletedAtIsNotNullAndDeletedAtIsNull(1L))
                .thenReturn(false);
        when(childRepository.existsByParentIdAndOnboardingCompletedAtIsNotNullAndDeletedAtIsNull(2L))
                .thenReturn(true);

        assertEquals("new-user", service.getStatus(1L).state());
        assertEquals("returning-user", service.getStatus(2L).state());

        verify(childRepository).existsByParentIdAndOnboardingCompletedAtIsNotNullAndDeletedAtIsNull(1L);
        verify(childRepository).existsByParentIdAndOnboardingCompletedAtIsNotNullAndDeletedAtIsNull(2L);
    }

    @Test
    void returningUserReceivesCurrentChildAndLatestSelectedActivity() {
        Child child = child(7L, 1L, "Ada");
        Activity activity = new Activity();
        ReflectionTestUtils.setField(activity, "id", 23L);
        ReflectionTestUtils.setField(activity, "title", "Animal Walk");
        DailyPlan plan = new DailyPlan(child.getId(), LocalDate.now());
        plan.add(activity, PlanSlotType.EXPLORE, BigDecimal.ONE);
        plan.select(activity.getId());
        DailyPlanItem item = plan.getItems().getFirst();
        ReflectionTestUtils.setField(item, "id", 41L);

        when(childRepository.existsByParentIdAndOnboardingCompletedAtIsNotNullAndDeletedAtIsNull(1L))
                .thenReturn(true);
        when(dailyPlanItemRepository.findLatestSelectedByParentId(any(Long.class), any(Pageable.class)))
                .thenReturn(List.of(item));
        when(childRepository.findByIdAndParentIdAndDeletedAtIsNull(7L, 1L)).thenReturn(Optional.of(child));
        when(childService.displayName(any(Child.class), any(Integer.class))).thenReturn("Ada");

        HomeStatusResponse response = service.getStatus(1L);

        assertEquals("returning-user", response.state());
        assertEquals(7L, response.childId());
        assertEquals("Ada", response.childName());
        assertNotNull(response.latestActivity());
        assertEquals(41L, response.latestActivity().dailyPlanItemId());
        assertEquals(23L, response.latestActivity().activityId());
        assertEquals("Animal Walk", response.latestActivity().title());
        assertNotNull(response.latestActivity().selectedAt());
    }

    private Child child(Long id, Long parentId, String fullName) {
        Child child = new Child();
        child.setId(id);
        child.setParentId(parentId);
        child.setFullName(fullName);
        child.setBirthDate(LocalDate.now().minusMonths(18));
        child.setOnboardingCompletedAt(OffsetDateTime.now());
        return child;
    }
}
