package com.kinloop.backend.service;

import com.kinloop.backend.dto.onboarding.IdentityQuestionResponse;
import com.kinloop.backend.dto.onboarding.IdentityQuestionsResponse;
import com.kinloop.backend.dto.onboarding.DailyTimeBudgetResponse;
import com.kinloop.backend.dto.onboarding.DailyTimeBudgetRange;
import com.kinloop.backend.dto.onboarding.OnboardingClosingMessageResponse;
import com.kinloop.backend.dto.profile.DailyTimeBudgetOptionResponse;
import com.kinloop.backend.dto.profile.DailyTimeBudgetProfileResponse;
import com.kinloop.backend.dto.questionnaire.QuestionOptionResponse;
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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private static final String DAILY_TIME_BUDGET_QUESTION_CODE = "Q7";

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final ChildRepository childRepository;
    private final DailyPlanRepository dailyPlanRepository;

    @Transactional(readOnly = true)
    public IdentityQuestionsResponse getIdentityQuestions() {
        List<Question> questions = questionRepository
                .findByScopeAndActiveTrueOrderByDisplayOrderAsc(QuestionScope.IDENTITY);
        if (questions.isEmpty()) {
            throw new IllegalStateException("Onboarding questions are missing");
        }
        List<IdentityQuestionResponse> responses = questions.stream()
                .map(this::toResponse)
                .toList();
        return new IdentityQuestionsResponse(responses);
    }

    @Transactional
    public DailyTimeBudgetResponse updateDailyTimeBudget(Child child, String optionCode) {
        QuestionOption option = dailyTimeBudgetOption(optionCode, child.ageInMonths(LocalDate.now()));
        DailyTimeBudgetRange range = requiredTimeBudget(option);
        child.setDailyTimeBudgetMin(range.minMinutes());
        child.setDailyTimeBudgetMax(range.maxMinutes());
        childRepository.save(child);
        return new DailyTimeBudgetResponse(option.getCode(), range.minMinutes(), range.maxMinutes());
    }

    @Transactional(readOnly = true)
    public DailyTimeBudgetProfileResponse getDailyTimeBudget(Child child) {
        Question question = dailyTimeBudgetQuestion();
        int ageMonths = child.ageInMonths(LocalDate.now());
        List<DailyTimeBudgetOptionResponse> options = question.getOptions().stream()
                .filter(option -> availableForAge(option, ageMonths))
                .map(this::toBudgetOptionResponse)
                .toList();
        return new DailyTimeBudgetProfileResponse(
                question.getCode(),
                question.getBody(),
                answeredOptionCode(question, child),
                child.getDailyTimeBudgetMin(),
                child.getDailyTimeBudgetMax(),
                options);
    }

    @Transactional(readOnly = true)
    public DailyTimeBudgetRange resolveDailyTimeBudget(String optionCode, int ageMonths) {
        return requiredTimeBudget(dailyTimeBudgetOption(optionCode, ageMonths));
    }

    @Transactional(readOnly = true)
    public OnboardingClosingMessageResponse getClosingMessage(Child child) {
        return closingMessageResponse(child);
    }

    @Transactional
    public OnboardingClosingMessageResponse respondToClosingMessage(
            Child child,
            OnboardingClosingAction action
    ) {
        Child lockedChild = childRepository.findLockedById(child.getId())
                .orElseThrow(() -> new IllegalStateException("Child not found"));
        if (lockedChild.getOnboardingCompletedAt() == null) {
            throw new IllegalArgumentException("Onboarding must be completed before responding to its closing message");
        }
        if (lockedChild.getOnboardingClosingMessageRespondedAt() == null) {
            long generatedPlanCount = dailyPlanRepository.countByChildId(lockedChild.getId());
            lockedChild.setOnboardingClosingMessageRespondedAt(OffsetDateTime.now());
            lockedChild.setOnboardingClosingReminderRequested(action == OnboardingClosingAction.REMIND_LATER);
            lockedChild.setOnboardingClosingReminderPlanBaseline(
                    action == OnboardingClosingAction.REMIND_LATER
                            ? Math.toIntExact(generatedPlanCount)
                            : null);
            childRepository.save(lockedChild);
        }
        return closingMessageResponse(lockedChild);
    }

    public boolean shouldShowPlanReminder(Child child, long generatedPlanCount) {
        Integer baseline = child.getOnboardingClosingReminderPlanBaseline();
        return child.isOnboardingClosingReminderRequested()
                && baseline != null
                && generatedPlanCount > baseline
                && generatedPlanCount <= baseline + 3L;
    }

    private OnboardingClosingMessageResponse closingMessageResponse(Child child) {
        long planCount = dailyPlanRepository.countByChildId(child.getId());
        Integer baseline = child.getOnboardingClosingReminderPlanBaseline();
        long reminderPlanCount = baseline == null ? 0 : Math.max(0, planCount - baseline);
        int remaining = child.isOnboardingClosingReminderRequested() && baseline != null
                ? Math.max(0, 3 - Math.toIntExact(Math.min(reminderPlanCount, 3)))
                : 0;
        return new OnboardingClosingMessageResponse(
                child.getOnboardingCompletedAt() != null
                        && child.getOnboardingClosingMessageRespondedAt() == null,
                remaining > 0,
                remaining);
    }

    private QuestionOption dailyTimeBudgetOption(String optionCode, int ageMonths) {
        Question question = dailyTimeBudgetQuestion();
        QuestionOption option = questionOptionRepository.findByQuestionIdAndCode(question.getId(), optionCode)
                .orElseThrow(() -> new InvalidOptionException(optionCode));
        if (!availableForAge(option, ageMonths)) {
            throw new InvalidOptionException(optionCode);
        }
        return option;
    }

    private Question dailyTimeBudgetQuestion() {
        return questionRepository
                .findByCodeAndScopeAndActiveTrue(DAILY_TIME_BUDGET_QUESTION_CODE, QuestionScope.CHILD_BUDGET)
                .orElseThrow(() -> new IllegalStateException("Daily time budget question is missing"));
    }

    private DailyTimeBudgetRange requiredTimeBudget(QuestionOption option) {
        if (option.getDailyTimeBudgetMin() == null || option.getDailyTimeBudgetMax() == null) {
            throw new IllegalStateException("Daily time budget mapping is missing for option " + option.getCode());
        }
        return new DailyTimeBudgetRange(option.getDailyTimeBudgetMin(), option.getDailyTimeBudgetMax());
    }

    private boolean availableForAge(QuestionOption option, int ageMonths) {
        return ageMonths >= 24 || !"C".equals(option.getCode());
    }

    private DailyTimeBudgetOptionResponse toBudgetOptionResponse(QuestionOption option) {
        DailyTimeBudgetRange range = requiredTimeBudget(option);
        return new DailyTimeBudgetOptionResponse(
                option.getCode(), option.getLabel(), option.getDisplayOrder(),
                range.minMinutes(), range.maxMinutes());
    }

    private IdentityQuestionResponse toResponse(Question question) {
        return new IdentityQuestionResponse(
                question.getCode(),
                question.getDisplayOrder(),
                question.getBody(),
                question.getHelperText(),
                question.getAnswerKey(),
                question.getMaxLength(),
                question.getQuestionType(),
                question.isRequired(),
                question.getOptions().stream()
                        .map(option -> new QuestionOptionResponse(
                                option.getCode(), option.getLabel(), option.getDisplayOrder()))
                        .toList(),
                null
        );
    }

    private String answeredOptionCode(Question question, Child child) {
        return question.getOptions().stream()
                .filter(option -> option.getDailyTimeBudgetMin() != null && option.getDailyTimeBudgetMax() != null)
                .filter(option -> child.getDailyTimeBudgetMin() == option.getDailyTimeBudgetMin()
                        && child.getDailyTimeBudgetMax() == option.getDailyTimeBudgetMax())
                .map(QuestionOption::getCode)
                .findFirst()
                .orElse(null);
    }
}
