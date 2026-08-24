package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinloop.backend.dto.onboarding.DailyTimeBudgetResponse;
import com.kinloop.backend.dto.onboarding.OnboardingClosingMessageResponse;
import com.kinloop.backend.dto.profile.DailyTimeBudgetProfileResponse;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.Question;
import com.kinloop.backend.entity.QuestionOption;
import com.kinloop.backend.entity.enums.OnboardingClosingAction;
import com.kinloop.backend.entity.enums.QuestionScope;
import com.kinloop.backend.exception.InvalidOptionException;
import com.kinloop.backend.repository.ChildRepository;
import com.kinloop.backend.repository.DailyPlanRepository;
import com.kinloop.backend.repository.QuestionOptionRepository;
import com.kinloop.backend.repository.QuestionRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionOptionRepository questionOptionRepository;
    @Mock private ChildRepository childRepository;
    @Mock private DailyPlanRepository dailyPlanRepository;

    private OnboardingService service;

    @BeforeEach
    void setUp() {
        service = new OnboardingService(
                questionRepository, questionOptionRepository, childRepository, dailyPlanRepository);
    }

    @Test
    void budgetUpdateWritesRangeToTheRequestedChild() {
        Question question = dailyTimeBudgetQuestion();
        QuestionOption option = question.getOptions().get(1);
        Child child = child(30);
        when(questionRepository.findByCodeAndScopeAndActiveTrue("Q7", QuestionScope.CHILD_BUDGET))
                .thenReturn(Optional.of(question));
        when(questionOptionRepository.findByQuestionIdAndCode(7L, "B")).thenReturn(Optional.of(option));

        DailyTimeBudgetResponse response = service.updateDailyTimeBudget(child, "B");

        assertEquals("B", response.answeredOptionCode());
        assertEquals((short) 25, response.minMinutes());
        assertEquals((short) 35, response.maxMinutes());
        assertEquals((short) 25, child.getDailyTimeBudgetMin());
        assertEquals((short) 35, child.getDailyTimeBudgetMax());
        org.junit.jupiter.api.Assertions.assertNotNull(child.getDailyTimeBudgetAnsweredAt());
        verify(childRepository).save(child);
    }

    @Test
    void infantBudgetOptionsExcludeC() {
        Question question = dailyTimeBudgetQuestion();
        Child child = child(18);
        child.setDailyTimeBudgetMin((short) 15);
        child.setDailyTimeBudgetMax((short) 25);
        child.setDailyTimeBudgetAnsweredAt(java.time.OffsetDateTime.now());
        when(questionRepository.findByCodeAndScopeAndActiveTrue("Q7", QuestionScope.CHILD_BUDGET))
                .thenReturn(Optional.of(question));

        DailyTimeBudgetProfileResponse response = service.getDailyTimeBudget(child);

        assertEquals("A", response.selectedOptionCode());
        assertEquals(2, response.options().size());
        assertEquals("B", response.options().getLast().code());
    }

    @Test
    void infantCannotSubmitOptionC() {
        Question question = dailyTimeBudgetQuestion();
        QuestionOption option = question.getOptions().getLast();
        when(questionRepository.findByCodeAndScopeAndActiveTrue("Q7", QuestionScope.CHILD_BUDGET))
                .thenReturn(Optional.of(question));
        when(questionOptionRepository.findByQuestionIdAndCode(7L, "C")).thenReturn(Optional.of(option));

        assertThrows(InvalidOptionException.class,
                () -> service.updateDailyTimeBudget(child(18), "C"));
    }

    @Test
    void remindLaterEnablesOnlyTheFirstThreeGeneratedPlans() {
        Child child = child(30);
        child.setOnboardingCompletedAt(OffsetDateTime.now());
        when(childRepository.findLockedById(child.getId())).thenReturn(Optional.of(child));
        when(dailyPlanRepository.countByChildId(child.getId())).thenReturn(5L, 5L, 7L, 8L);

        OnboardingClosingMessageResponse initial = service.getClosingMessage(child);
        OnboardingClosingMessageResponse deferred = service.respondToClosingMessage(
                child, OnboardingClosingAction.REMIND_LATER);
        OnboardingClosingMessageResponse exhausted = service.getClosingMessage(child);

        assertTrue(initial.shouldDisplay());
        assertFalse(deferred.shouldDisplay());
        assertTrue(deferred.planReminderEnabled());
        assertEquals(1, deferred.reminderPlansRemaining());
        assertFalse(exhausted.planReminderEnabled());
        assertEquals(0, exhausted.reminderPlansRemaining());
        assertTrue(service.shouldShowPlanReminder(child, 6));
        assertTrue(service.shouldShowPlanReminder(child, 8));
        assertFalse(service.shouldShowPlanReminder(child, 9));
    }

    @Test
    void firstClosingResponseWinsSoRetriesCannotChangeTheDecision() {
        Child child = child(30);
        child.setOnboardingCompletedAt(OffsetDateTime.now());
        when(childRepository.findLockedById(child.getId())).thenReturn(Optional.of(child));
        when(dailyPlanRepository.countByChildId(child.getId())).thenReturn(0L);

        service.respondToClosingMessage(child, OnboardingClosingAction.START);
        service.respondToClosingMessage(child, OnboardingClosingAction.REMIND_LATER);

        assertFalse(child.isOnboardingClosingReminderRequested());
    }

    private Question dailyTimeBudgetQuestion() {
        Question question = new Question();
        question.setId(7L);
        question.setCode("Q7");
        question.setBody("Çocuğunuzla etkinlik için genellikle ne kadar vakit ayırabiliyorsunuz?");
        question.getOptions().add(option(question, "A", "Kısa ve öz olsun", 1, (short) 15, (short) 25));
        question.getOptions().add(option(question, "B", "Yarım saatim var", 2, (short) 25, (short) 35));
        question.getOptions().add(option(question, "C", "Rahatça vakit ayırabiliriz", 3, (short) 35, (short) 45));
        return question;
    }

    private QuestionOption option(
            Question question, String code, String label, int order, short min, short max) {
        QuestionOption option = new QuestionOption();
        option.setQuestion(question);
        option.setCode(code);
        option.setLabel(label);
        option.setDisplayOrder(order);
        option.setDailyTimeBudgetMin(min);
        option.setDailyTimeBudgetMax(max);
        return option;
    }

    private Child child(int ageMonths) {
        Child child = new Child();
        child.setId(9L);
        child.setBirthDate(LocalDate.now().minusMonths(ageMonths));
        return child;
    }
}
