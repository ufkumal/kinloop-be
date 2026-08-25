package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.kinloop.backend.dto.home.HomeStatusResponse;
import com.kinloop.backend.dto.questionnaire.CurrentQuestionnaireResponse;
import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.ActivityInstruction;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.DailyPlan;
import com.kinloop.backend.entity.DailyPlanItem;
import com.kinloop.backend.entity.Feedback;
import com.kinloop.backend.entity.enums.FeedbackReason;
import com.kinloop.backend.entity.enums.FeedbackType;
import com.kinloop.backend.entity.enums.PlanSlotType;
import com.kinloop.backend.entity.enums.AgeBand;
import com.kinloop.backend.entity.enums.SessionStatus;
import com.kinloop.backend.entity.enums.TriggerReason;
import com.kinloop.backend.repository.ChildRepository;
import com.kinloop.backend.repository.DailyPlanItemRepository;
import com.kinloop.backend.repository.DailyPlanRepository;
import com.kinloop.backend.repository.FeedbackRepository;
import com.kinloop.backend.repository.ParentProfileRepository;
import com.kinloop.backend.entity.ParentProfile;
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
    @Mock private DailyPlanRepository dailyPlanRepository;
    @Mock private FeedbackRepository feedbackRepository;
    @Mock private ChildService childService;
    @Mock private ParentProfileRepository parentProfileRepository;
    @Mock private ConsentService consentService;
    @Mock private QuestionnaireSessionService questionnaireSessionService;

    private HomeService service;

    @BeforeEach
    void setUp() {
        service = new HomeService(
                childRepository, dailyPlanItemRepository, dailyPlanRepository,
                feedbackRepository, childService,
                parentProfileRepository, consentService, questionnaireSessionService);
        ParentProfile parent = new ParentProfile();
        parent.setId(1L);
        parent.setUserId(101L);
        lenient().when(parentProfileRepository.findById(1L)).thenReturn(Optional.of(parent));
        lenient().when(consentService.firstMissingRequiredConsentId(101L)).thenReturn(Optional.empty());
    }

    @Test
    void parentWithoutChildReceivesNewUser() {
        when(childRepository.findByParentIdAndDeletedAtIsNullOrderByIdAsc(1L)).thenReturn(List.of());

        HomeStatusResponse response = service.getStatus(1L);

        assertEquals("new-user", response.state());
    }

    @Test
    void parentWithIncompleteOnboardingReceivesHalfOnboardingUser() {
        Child child = child(7L, 1L, "Ada");
        child.setOnboardingCompletedAt(null);
        when(childRepository.findByParentIdAndDeletedAtIsNullOrderByIdAsc(1L)).thenReturn(List.of(child));
        when(childService.displayName(any(Child.class), any(Integer.class))).thenReturn("Ada");

        HomeStatusResponse response = service.getStatus(1L);

        assertEquals("half-onboarding-user", response.state());
        assertEquals("DAILY_TIME_BUDGET", response.onboardingStep().name());
        assertEquals(7L, response.childId());
    }

    @Test
    void incompleteQuestionnaireReturnsTheExactNextQuestion() {
        Child child = child(7L, 1L, "Ada");
        child.setOnboardingCompletedAt(null);
        child.setDailyTimeBudgetAnsweredAt(OffsetDateTime.now());
        when(childRepository.findByParentIdAndDeletedAtIsNullOrderByIdAsc(1L)).thenReturn(List.of(child));
        when(childService.displayName(any(Child.class), any(Integer.class))).thenReturn("Ada");
        when(questionnaireSessionService.current(child)).thenReturn(new CurrentQuestionnaireResponse(
                3L, SessionStatus.IN_PROGRESS, TriggerReason.INITIAL, AgeBand.BAND_12_24,
                18, 4, 2, "Q4", List.of()));

        HomeStatusResponse response = service.getStatus(1L);

        assertEquals("half-onboarding-user", response.state());
        assertEquals("QUESTIONNAIRE", response.onboardingStep().name());
        assertEquals("Q4", response.nextQuestionCode());
    }

    @Test
    void completedQuestionnaireWithoutRequiredConsentsResumesAtConsents() {
        Child child = child(7L, 1L, "Ada");
        when(childRepository.findByParentIdAndDeletedAtIsNullOrderByIdAsc(1L)).thenReturn(List.of(child));
        when(childService.displayName(any(Child.class), any(Integer.class))).thenReturn("Ada");
        when(consentService.firstMissingRequiredConsentId(101L)).thenReturn(Optional.of(5L));

        HomeStatusResponse response = service.getStatus(1L);

        assertEquals("half-onboarding-user", response.state());
        assertEquals("CONSENTS", response.onboardingStep().name());
        assertEquals(5L, response.nextConsentId());
        assertFalse(response.shouldGenerateDailyPlan());
    }

    @Test
    void parentWithCompletedOnboardingReceivesReturningUser() {
        Child child = child(7L, 1L, "Ada");
        when(childRepository.findByParentIdAndDeletedAtIsNullOrderByIdAsc(1L)).thenReturn(List.of(child));
        when(childService.displayName(any(Child.class), any(Integer.class))).thenReturn("Ada");

        HomeStatusResponse response = service.getStatus(1L);

        assertEquals("returning-user", response.state());
    }

    @Test
    void statusIsScopedToAuthenticatedParentProfile() {
        when(childRepository.findByParentIdAndDeletedAtIsNullOrderByIdAsc(1L)).thenReturn(List.of());

        assertEquals("new-user", service.getStatus(1L).state());
        verify(childRepository).findByParentIdAndDeletedAtIsNullOrderByIdAsc(1L);
    }

    @Test
    void selectedActivityWithoutFeedbackRequiresFeedback() {
        Child child = child(7L, 1L, "Ada");
        Activity activity = new Activity();
        ReflectionTestUtils.setField(activity, "id", 23L);
        ReflectionTestUtils.setField(activity, "title", "Animal Walk");
        DailyPlan plan = new DailyPlan(child.getId(), LocalDate.now());
        plan.add(activity, PlanSlotType.EXPLORE, BigDecimal.ONE);
        plan.select(activity.getId());
        DailyPlanItem item = plan.getItems().getFirst();
        ReflectionTestUtils.setField(item, "id", 41L);

        when(childRepository.findByParentIdAndDeletedAtIsNullOrderByIdAsc(1L)).thenReturn(List.of(child));
        when(dailyPlanItemRepository.findLatestSelectedByParentId(any(Long.class), any(Pageable.class)))
                .thenReturn(List.of(item));
        when(childRepository.findByIdAndParentIdAndDeletedAtIsNull(7L, 1L)).thenReturn(Optional.of(child));
        when(feedbackRepository.findByChildIdAndDailyPlanItemId(7L, 41L))
                .thenReturn(Optional.empty());
        when(childService.displayName(any(Child.class), any(Integer.class))).thenReturn("Ada");

        HomeStatusResponse response = service.getStatus(1L);

        assertEquals("feedback-required", response.state());
        assertEquals(7L, response.childId());
        assertEquals("Ada", response.childName());
        assertNotNull(response.latestActivity());
        assertEquals(41L, response.latestActivity().dailyPlanItemId());
        assertEquals(23L, response.latestActivity().activityId());
        assertEquals("Animal Walk", response.latestActivity().title());
        assertNotNull(response.latestActivity().selectedAt());
        assertFalse(response.shouldGenerateDailyPlan());
        assertFalse(response.shouldListExistingPlan());
    }

    @Test
    void completedActivityReturnsItsFeedbackAndLeadsToPlanGeneration() {
        Child child = child(7L, 1L, "Ada");
        Activity activity = new Activity();
        ReflectionTestUtils.setField(activity, "id", 23L);
        ReflectionTestUtils.setField(activity, "title", "Animal Walk");
        ReflectionTestUtils.setField(activity, "description", "Move like an animal");
        ReflectionTestUtils.setField(activity, "durationMinutes", (short) 10);
        ActivityInstruction instruction = new ActivityInstruction();
        ReflectionTestUtils.setField(instruction, "intro", "Choose an animal");
        ReflectionTestUtils.setField(instruction, "purpose", "Practice movement");
        ReflectionTestUtils.setField(activity, "instruction", instruction);
        DailyPlan plan = new DailyPlan(child.getId(), LocalDate.now());
        ReflectionTestUtils.setField(plan, "id", 31L);
        plan.add(activity, PlanSlotType.EXPLORE, BigDecimal.ONE);
        plan.select(activity.getId());
        DailyPlanItem item = plan.getItems().getFirst();
        ReflectionTestUtils.setField(item, "id", 41L);
        item.complete();
        Feedback feedback = new Feedback(
                child.getId(), item, FeedbackType.LIKED, FeedbackReason.INTEREST, "She loved it");
        ReflectionTestUtils.setField(feedback, "id", 91L);
        ReflectionTestUtils.setField(feedback, "createdAt", OffsetDateTime.now());

        when(childRepository.findByParentIdAndDeletedAtIsNullOrderByIdAsc(1L)).thenReturn(List.of(child));
        when(dailyPlanItemRepository.findLatestSelectedByParentId(any(Long.class), any(Pageable.class)))
                .thenReturn(List.of(item));
        when(childRepository.findByIdAndParentIdAndDeletedAtIsNull(7L, 1L)).thenReturn(Optional.of(child));
        when(feedbackRepository.findByChildIdAndDailyPlanItemId(7L, 41L))
                .thenReturn(Optional.of(feedback));
        when(dailyPlanRepository.findFirstByChildIdAndPlanDateOrderByIdDesc(7L, LocalDate.now()))
                .thenReturn(Optional.of(plan));
        when(childService.displayName(any(Child.class), any(Integer.class))).thenReturn("Ada");

        HomeStatusResponse response = service.getStatus(1L);

        assertEquals("returning-user", response.state());
        assertEquals("Choose an animal", response.latestActivity().intro());
        assertEquals("Practice movement", response.latestActivity().purpose());
        assertNotNull(response.latestActivity().completedAt());
        assertTrue(response.latestActivity().feedbackSubmitted());
        assertEquals(FeedbackType.LIKED, response.latestActivity().feedback().feedbackType());
        assertEquals(FeedbackReason.INTEREST, response.latestActivity().feedback().resolvedReason());
        assertEquals("She loved it", response.latestActivity().feedback().freeText());
        assertTrue(response.shouldGenerateDailyPlan());
        assertFalse(response.shouldListExistingPlan());
    }

    @Test
    void completedActivityDoesNotRequestANewRoundWhileCurrentRoundHasPendingItems() {
        Child child = child(7L, 1L, "Ada");
        Activity activity = new Activity();
        ReflectionTestUtils.setField(activity, "id", 23L);
        DailyPlan plan = new DailyPlan(child.getId(), LocalDate.now());
        ReflectionTestUtils.setField(plan, "id", 31L);
        plan.add(activity, PlanSlotType.EXPLORE, BigDecimal.ONE);
        Activity pendingActivity = new Activity();
        ReflectionTestUtils.setField(pendingActivity, "id", 24L);
        plan.add(pendingActivity, PlanSlotType.DEVELOP, BigDecimal.TEN);
        plan.select(activity.getId());
        DailyPlanItem item = plan.getItems().getFirst();
        ReflectionTestUtils.setField(item, "id", 41L);
        item.complete();

        when(childRepository.findByParentIdAndDeletedAtIsNullOrderByIdAsc(1L)).thenReturn(List.of(child));
        when(dailyPlanItemRepository.findLatestSelectedByParentId(any(Long.class), any(Pageable.class)))
                .thenReturn(List.of(item));
        when(childRepository.findByIdAndParentIdAndDeletedAtIsNull(7L, 1L)).thenReturn(Optional.of(child));
        when(feedbackRepository.findByChildIdAndDailyPlanItemId(7L, 41L)).thenReturn(Optional.empty());
        when(dailyPlanRepository.findFirstByChildIdAndPlanDateOrderByIdDesc(7L, LocalDate.now()))
                .thenReturn(Optional.of(plan));
        when(childService.displayName(any(Child.class), any(Integer.class))).thenReturn("Ada");

        HomeStatusResponse response = service.getStatus(1L);

        assertFalse(response.shouldGenerateDailyPlan());
        assertTrue(response.shouldListExistingPlan());
    }

    @Test
    void noSelectedActivityLeadsToPlanGeneration() {
        Child child = child(7L, 1L, "Ada");
        when(childRepository.findByParentIdAndDeletedAtIsNullOrderByIdAsc(1L))
                .thenReturn(List.of(child));
        when(childService.displayName(any(Child.class), any(Integer.class))).thenReturn("Ada");

        HomeStatusResponse response = service.getStatus(1L);

        assertEquals("returning-user", response.state());
        assertNull(response.latestActivity());
        assertTrue(response.shouldGenerateDailyPlan());
        assertFalse(response.shouldListExistingPlan());
    }

    @Test
    void unselectedExistingRoundIsListedInsteadOfRegenerated() {
        Child child = child(7L, 1L, "Ada");
        DailyPlan plan = new DailyPlan(child.getId(), LocalDate.now());
        Activity activity = new Activity();
        ReflectionTestUtils.setField(activity, "id", 23L);
        plan.add(activity, PlanSlotType.EXPLORE, BigDecimal.ONE);
        when(childRepository.findByParentIdAndDeletedAtIsNullOrderByIdAsc(1L))
                .thenReturn(List.of(child));
        when(dailyPlanRepository.findFirstByChildIdAndPlanDateOrderByIdDesc(7L, LocalDate.now()))
                .thenReturn(Optional.of(plan));
        when(childService.displayName(any(Child.class), any(Integer.class))).thenReturn("Ada");

        HomeStatusResponse response = service.getStatus(1L);

        assertFalse(response.shouldGenerateDailyPlan());
        assertTrue(response.shouldListExistingPlan());
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
