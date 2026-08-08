package com.kinloop.backend.service;

import com.kinloop.backend.dto.onboarding.IdentityQuestionResponse;
import com.kinloop.backend.dto.onboarding.IdentityQuestionsResponse;
import com.kinloop.backend.dto.onboarding.DailyTimeBudgetResponse;
import com.kinloop.backend.dto.questionnaire.QuestionOptionResponse;
import com.kinloop.backend.entity.ParentProfile;
import com.kinloop.backend.entity.Question;
import com.kinloop.backend.entity.QuestionOption;
import com.kinloop.backend.entity.enums.QuestionScope;
import com.kinloop.backend.exception.InvalidOptionException;
import com.kinloop.backend.repository.ParentProfileRepository;
import com.kinloop.backend.repository.QuestionOptionRepository;
import com.kinloop.backend.repository.QuestionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private static final String DAILY_TIME_BUDGET_QUESTION_CODE = "Q7";

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final ParentProfileRepository parentProfileRepository;

    @Transactional(readOnly = true)
    public IdentityQuestionsResponse getIdentityQuestions(Long parentProfileId) {
        List<Question> questions = Stream.concat(
                        questionRepository.findByScopeAndActiveTrueOrderByDisplayOrderAsc(QuestionScope.IDENTITY).stream(),
                        questionRepository.findByScopeAndActiveTrueOrderByDisplayOrderAsc(QuestionScope.HOUSEHOLD).stream())
                .sorted(Comparator.comparingInt(Question::getDisplayOrder))
                .toList();
        if (questions.isEmpty()) {
            throw new IllegalStateException("Onboarding questions are missing");
        }
        ParentProfile parentProfile = parentProfile(parentProfileId);
        List<IdentityQuestionResponse> responses = questions.stream()
                .map(question -> toResponse(question, parentProfile))
                .toList();
        return new IdentityQuestionsResponse(responses, parentProfile.getDailyTimeBudgetMinutes());
    }

    @Transactional
    public DailyTimeBudgetResponse updateDailyTimeBudget(Long parentProfileId, String optionCode) {
        QuestionOption option = dailyTimeBudgetOption(optionCode);
        Short minutes = requiredTimeBudget(option);
        ParentProfile parentProfile = parentProfile(parentProfileId);
        parentProfile.setDailyTimeBudgetMinutes(minutes);
        parentProfileRepository.save(parentProfile);
        return new DailyTimeBudgetResponse(option.getCode(), minutes);
    }

    @Transactional(readOnly = true)
    public Short resolveDailyTimeBudget(String optionCode) {
        return requiredTimeBudget(dailyTimeBudgetOption(optionCode));
    }

    private ParentProfile parentProfile(Long parentProfileId) {
        return parentProfileRepository.findById(parentProfileId)
                .filter(profile -> profile.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalStateException("Parent profile not found"));
    }

    private QuestionOption dailyTimeBudgetOption(String optionCode) {
        Question question = questionRepository
                .findByCodeAndScopeAndActiveTrue(DAILY_TIME_BUDGET_QUESTION_CODE, QuestionScope.HOUSEHOLD)
                .orElseThrow(() -> new IllegalStateException("Daily time budget question is missing"));
        return questionOptionRepository.findByQuestionIdAndCode(question.getId(), optionCode)
                .orElseThrow(() -> new InvalidOptionException(optionCode));
    }

    private Short requiredTimeBudget(QuestionOption option) {
        if (option.getDailyTimeBudgetMinutes() == null) {
            throw new IllegalStateException("Daily time budget mapping is missing for option " + option.getCode());
        }
        return option.getDailyTimeBudgetMinutes();
    }

    private IdentityQuestionResponse toResponse(Question question, ParentProfile parentProfile) {
        return new IdentityQuestionResponse(
                question.getCode(),
                question.getDisplayOrder(),
                question.getBody(),
                question.getQuestionType(),
                question.isRequired(),
                question.getOptions().stream()
                        .map(option -> new QuestionOptionResponse(
                                option.getCode(), option.getLabel(), option.getDisplayOrder()))
                        .toList(),
                answeredOptionCode(question, parentProfile)
        );
    }

    private String answeredOptionCode(Question question, ParentProfile parentProfile) {
        if (!DAILY_TIME_BUDGET_QUESTION_CODE.equals(question.getCode())
                || parentProfile.getDailyTimeBudgetMinutes() == null) {
            return null;
        }
        return question.getOptions().stream()
                .filter(option -> parentProfile.getDailyTimeBudgetMinutes().equals(option.getDailyTimeBudgetMinutes()))
                .map(QuestionOption::getCode)
                .findFirst()
                .orElse(null);
    }
}
