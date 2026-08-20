package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinloop.backend.dto.onboarding.DailyTimeBudgetResponse;
import com.kinloop.backend.dto.profile.DailyTimeBudgetProfileResponse;
import com.kinloop.backend.entity.ParentProfile;
import com.kinloop.backend.entity.Question;
import com.kinloop.backend.entity.QuestionOption;
import com.kinloop.backend.entity.enums.QuestionScope;
import com.kinloop.backend.repository.ParentProfileRepository;
import com.kinloop.backend.repository.QuestionOptionRepository;
import com.kinloop.backend.repository.QuestionRepository;
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
    @Mock private ParentProfileRepository parentProfileRepository;

    private OnboardingService service;

    @BeforeEach
    void setUp() {
        service = new OnboardingService(questionRepository, questionOptionRepository, parentProfileRepository);
    }

    @Test
    void optionCodeIsResolvedToServerSideTimeBudgetMapping() {
        Question question = new Question();
        question.setId(7L);
        QuestionOption option = new QuestionOption();
        option.setCode("B");
        option.setDailyTimeBudgetMinutes((short) 20);
        ParentProfile parent = new ParentProfile();
        parent.setId(1L);

        when(questionRepository.findByCodeAndScopeAndActiveTrue("Q7", QuestionScope.HOUSEHOLD))
                .thenReturn(Optional.of(question));
        when(questionOptionRepository.findByQuestionIdAndCode(7L, "B")).thenReturn(Optional.of(option));
        when(parentProfileRepository.findById(1L)).thenReturn(Optional.of(parent));

        DailyTimeBudgetResponse response = service.updateDailyTimeBudget(1L, "B");

        assertEquals("B", response.answeredOptionCode());
        assertEquals(Short.valueOf((short) 20), response.dailyTimeBudgetMinutes());
        assertEquals(Short.valueOf((short) 20), parent.getDailyTimeBudgetMinutes());
        verify(parentProfileRepository).save(parent);
    }

    @Test
    void dailyTimeBudgetReturnsOrderedDatabaseOptionsAndCurrentSelection() {
        Question question = dailyTimeBudgetQuestion();
        ParentProfile parent = parent((short) 20);
        when(questionRepository.findByCodeAndScopeAndActiveTrue("Q7", QuestionScope.HOUSEHOLD))
                .thenReturn(Optional.of(question));
        when(parentProfileRepository.findById(1L)).thenReturn(Optional.of(parent));

        DailyTimeBudgetProfileResponse response = service.getDailyTimeBudget(1L);

        assertEquals("Q7", response.questionCode());
        assertEquals("B", response.selectedOptionCode());
        assertEquals(Short.valueOf((short) 20), response.dailyTimeBudgetMinutes());
        assertEquals(3, response.options().size());
        assertEquals("A", response.options().getFirst().code());
        assertEquals(Short.valueOf((short) 10), response.options().getFirst().minutes());
        assertEquals("C", response.options().getLast().code());
        assertEquals(Short.valueOf((short) 30), response.options().getLast().minutes());
    }

    @Test
    void dailyTimeBudgetKeepsNewcomerSelectionNull() {
        Question question = dailyTimeBudgetQuestion();
        when(questionRepository.findByCodeAndScopeAndActiveTrue("Q7", QuestionScope.HOUSEHOLD))
                .thenReturn(Optional.of(question));
        when(parentProfileRepository.findById(1L)).thenReturn(Optional.of(parent(null)));

        DailyTimeBudgetProfileResponse response = service.getDailyTimeBudget(1L);

        assertNull(response.selectedOptionCode());
        assertNull(response.dailyTimeBudgetMinutes());
        assertEquals(3, response.options().size());
    }

    private Question dailyTimeBudgetQuestion() {
        Question question = new Question();
        question.setId(7L);
        question.setCode("Q7");
        question.setBody("Çocuğunuzla günde ne kadar vakit ayırabiliyorsunuz?");
        question.getOptions().add(option("A", "5-10 dk", 1, (short) 10));
        question.getOptions().add(option("B", "15-20 dk", 2, (short) 20));
        question.getOptions().add(option("C", "30+ dk", 3, (short) 30));
        return question;
    }

    private QuestionOption option(String code, String label, int displayOrder, short minutes) {
        QuestionOption option = new QuestionOption();
        option.setCode(code);
        option.setLabel(label);
        option.setDisplayOrder(displayOrder);
        option.setDailyTimeBudgetMinutes(minutes);
        return option;
    }

    private ParentProfile parent(Short minutes) {
        ParentProfile parent = new ParentProfile();
        parent.setId(1L);
        parent.setDailyTimeBudgetMinutes(minutes);
        return parent;
    }
}
