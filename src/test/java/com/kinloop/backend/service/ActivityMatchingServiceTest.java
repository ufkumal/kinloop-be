package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinloop.backend.dto.matching.DailyPlanResponse;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.DailyPlan;
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
import com.kinloop.backend.service.matching.ActivityEligibilityPolicy;
import com.kinloop.backend.service.matching.ActivityFreshnessPolicy;
import com.kinloop.backend.service.matching.ActivityScorer;
import com.kinloop.backend.service.matching.CandidateOrdering;
import com.kinloop.backend.service.matching.DailyPortfolioBuilder;
import com.kinloop.backend.service.matching.MatchingParameters;
import com.kinloop.backend.service.matching.MatchingStateInitializer;
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
    @InjectMocks private ActivityMatchingService service;

    @Test
    void sameDayRequestReturnsStoredPlanWithoutRecalculation() {
        Child child = new Child();
        child.setId(9L);
        DailyPlan stored = new DailyPlan(9L, LocalDate.now(), (short) 35, (short) 45);
        stored.recordBookkeeping(30, 40, (short) 2);
        ReflectionTestUtils.setField(stored, "id", 77L);
        when(childRepository.findLockedById(9L)).thenReturn(Optional.of(child));
        when(planRepository.findByChildIdAndPlanDate(9L, LocalDate.now())).thenReturn(Optional.of(stored));

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
    void storedFallbackFourPlanReturnsAnExplanatoryNonTechnicalState() {
        Child child = new Child();
        child.setId(9L);
        DailyPlan stored = new DailyPlan(9L, LocalDate.now());
        stored.recordBookkeeping(0, 0, (short) 4);
        when(childRepository.findLockedById(9L)).thenReturn(Optional.of(child));
        when(planRepository.findByChildIdAndPlanDate(9L, LocalDate.now())).thenReturn(Optional.of(stored));

        DailyPlanResponse response = service.today(child);

        assertEquals("EMPTY_POOL", response.state());
        assertNotNull(response.message());
        assertEquals(0, response.activities().size());
    }
}
