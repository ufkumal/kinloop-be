package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinloop.backend.dto.matching.DailyPlanResponse;
import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.DailyPlan;
import com.kinloop.backend.entity.DailyPlanItem;
import com.kinloop.backend.entity.enums.PlanSlotType;
import com.kinloop.backend.repository.ActivityRepository;
import com.kinloop.backend.repository.ChildDomainLevelRepository;
import com.kinloop.backend.repository.ChildIntelligenceScoreRepository;
import com.kinloop.backend.repository.ChildProfileSnapshotRepository;
import com.kinloop.backend.repository.ChildRepository;
import com.kinloop.backend.repository.ChildSensoryAdjustmentRepository;
import com.kinloop.backend.repository.DailyPlanRepository;
import com.kinloop.backend.repository.DevelopmentalPeriodTaskRepository;
import com.kinloop.backend.repository.DunnProfileRepository;
import com.kinloop.backend.repository.RecommendationRepository;
import com.kinloop.backend.repository.ParentProfileRepository;
import com.kinloop.backend.entity.ParentProfile;
import com.kinloop.backend.exception.RequiredConsentMissingException;
import com.kinloop.backend.service.matching.ActivityEligibilityPolicy;
import com.kinloop.backend.service.matching.ActivityFreshnessPolicy;
import com.kinloop.backend.service.matching.ActivityScorer;
import com.kinloop.backend.service.matching.CandidateOrdering;
import com.kinloop.backend.service.matching.DailyPortfolioBuilder;
import com.kinloop.backend.service.matching.MatchingParameters;
import com.kinloop.backend.service.matching.MatchingStateInitializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ActivityMatchingServiceTest {
    @Mock private ChildRepository childRepository;
    @Mock private ChildProfileSnapshotRepository profileRepository;
    @Mock private ChildSensoryAdjustmentRepository sensoryAdjustmentRepository;
    @Mock private ActivityRepository activityRepository;
    @Mock private DunnProfileRepository dunnRepository;
    @Mock private DevelopmentalPeriodTaskRepository periodRepository;
    @Mock private ChildIntelligenceScoreRepository intelligenceRepository;
    @Mock private ChildDomainLevelRepository domainRepository;
    @Mock private DailyPlanRepository planRepository;
    @Mock private RecommendationRepository recommendationRepository;
    @Mock private MatchingParameters parameters;
    @Mock private MatchingStateInitializer stateInitializer;
    @Mock private ActivityEligibilityPolicy eligibilityPolicy;
    @Mock private ActivityFreshnessPolicy freshnessPolicy;
    @Mock private ActivityScorer scorer;
    @Mock private CandidateOrdering candidateOrdering;
    @Mock private DailyPortfolioBuilder portfolioBuilder;
    @Mock private OnboardingService onboardingService;
    @Mock private ParentProfileRepository parentProfileRepository;
    @Mock private ConsentService consentService;
    @InjectMocks private ActivityMatchingService service;

    @Test
    void sameDayRequestReturnsStoredPlanWithoutRecalculation() {
        Child child = new Child();
        child.setId(9L);
        DailyPlan stored = new DailyPlan(9L, LocalDate.now(), (short) 35, (short) 45);
        stored.recordBookkeeping(30, 40, (short) 2);
        ReflectionTestUtils.setField(stored, "id", 77L);
        when(childRepository.findLockedById(9L)).thenReturn(Optional.of(child));
        when(planRepository.findFirstByChildIdAndPlanDateOrderByIdDesc(9L, LocalDate.now()))
                .thenReturn(Optional.of(stored));

        DailyPlanResponse response = service.today(child);

        assertEquals(77L, response.planId());
        assertEquals(35, response.budgetMin());
        assertEquals(45, response.budgetMax());
        assertEquals(30, response.committedDurationMinutes());
        assertEquals(40, response.totalDurationMinutes());
        assertEquals((short) 2, response.fallbackLevel());
        verify(activityRepository, never()).findEligibleBasePool(org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyShort());
        verify(parameters, never()).load();
    }

    @Test
    void storedPlanResponseDoesNotRepeatAnItemDuplicatedByOrmHydration() {
        Child child = new Child();
        child.setId(9L);
        Activity activity = new Activity();
        ReflectionTestUtils.setField(activity, "id", 15L);
        DailyPlan stored = new DailyPlan(9L, LocalDate.now());
        stored.add(activity, PlanSlotType.DEVELOP, BigDecimal.TEN);
        DailyPlanItem item = stored.getItems().getFirst();
        ReflectionTestUtils.setField(item, "id", 42L);
        stored.getItems().add(item);
        when(childRepository.findLockedById(9L)).thenReturn(Optional.of(child));
        when(planRepository.findFirstByChildIdAndPlanDateOrderByIdDesc(9L, LocalDate.now()))
                .thenReturn(Optional.of(stored));

        DailyPlanResponse response = service.today(child);

        assertEquals(1, response.activities().size());
        assertEquals(42L, response.activities().getFirst().dailyPlanItemId());
    }

    @Test
    void storedFallbackFourPlanReturnsAnExplanatoryNonTechnicalState() {
        Child child = new Child();
        child.setId(9L);
        DailyPlan stored = new DailyPlan(9L, LocalDate.now());
        stored.recordBookkeeping(0, 0, (short) 4);
        when(childRepository.findLockedById(9L)).thenReturn(Optional.of(child));
        when(planRepository.findFirstByChildIdAndPlanDateOrderByIdDesc(9L, LocalDate.now()))
                .thenReturn(Optional.of(stored));

        DailyPlanResponse response = service.today(child);

        assertEquals("EMPTY_POOL", response.state());
        assertNotNull(response.message());
        assertEquals(0, response.activities().size());
    }

    @Test
    void completedSameDayRoundContinuesToNewPlanGeneration() {
        Child child = new Child();
        child.setId(9L);
        child.setParentId(4L);
        DailyPlan completed = new DailyPlan(9L, LocalDate.now());
        completed.add(activity(15L), PlanSlotType.STRENGTHEN, BigDecimal.TEN);
        completed.add(activity(16L), PlanSlotType.DEVELOP, BigDecimal.ONE);
        completed.add(activity(17L), PlanSlotType.EXPLORE, BigDecimal.ZERO);
        completed.getItems().forEach(DailyPlanItem::complete);
        ParentProfile parent = new ParentProfile();
        parent.setId(4L);
        parent.setUserId(12L);
        when(childRepository.findLockedById(9L)).thenReturn(Optional.of(child));
        when(planRepository.findFirstByChildIdAndPlanDateOrderByIdDesc(9L, LocalDate.now()))
                .thenReturn(Optional.of(completed));
        when(parentProfileRepository.findById(4L)).thenReturn(Optional.of(parent));
        org.mockito.Mockito.doThrow(new RequiredConsentMissingException())
                .when(consentService).requireAllRequiredConsents(12L);

        assertThrows(RequiredConsentMissingException.class, () -> service.today(child));
        verify(parentProfileRepository).findById(4L);
    }

    @Test
    void selectionResponseContainsOnlyTheRequestedActivity() {
        Child child = new Child();
        child.setId(9L);
        DailyPlan plan = new DailyPlan(9L, LocalDate.now());
        plan.add(activity(15L), PlanSlotType.STRENGTHEN, BigDecimal.TEN);
        plan.add(activity(16L), PlanSlotType.DEVELOP, BigDecimal.ONE);
        plan.add(activity(17L), PlanSlotType.EXPLORE, BigDecimal.ZERO);
        when(planRepository.findFirstByChildIdAndPlanDateOrderByIdDesc(9L, LocalDate.now()))
                .thenReturn(Optional.of(plan));
        when(planRepository.save(plan)).thenReturn(plan);

        DailyPlanResponse response = service.selectActivity(child, 16L);

        assertEquals(1, response.activities().size());
        assertEquals(16L, response.activities().getFirst().activityId());
    }

    @Test
    void refusesToGenerateANewPlanWithoutRequiredConsents() {
        Child child = new Child();
        child.setId(9L);
        child.setParentId(4L);
        ParentProfile parent = new ParentProfile();
        parent.setId(4L);
        parent.setUserId(12L);
        when(childRepository.findLockedById(9L)).thenReturn(Optional.of(child));
        when(planRepository.findFirstByChildIdAndPlanDateOrderByIdDesc(9L, LocalDate.now()))
                .thenReturn(Optional.empty());
        when(parentProfileRepository.findById(4L)).thenReturn(Optional.of(parent));
        org.mockito.Mockito.doThrow(new RequiredConsentMissingException())
                .when(consentService).requireAllRequiredConsents(12L);

        assertThrows(RequiredConsentMissingException.class, () -> service.today(child));
        verify(profileRepository, never()).findByChildIdAndCurrentTrue(9L);
        verify(planRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private Activity activity(Long id) {
        Activity activity = new Activity();
        ReflectionTestUtils.setField(activity, "id", id);
        return activity;
    }
}
