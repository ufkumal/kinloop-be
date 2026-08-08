package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinloop.backend.dto.onboarding.DailyTimeBudgetResponse;
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
}
