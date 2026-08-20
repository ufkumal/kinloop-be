package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.kinloop.backend.dto.profile.ProfileResponse;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.ChildAnswer;
import com.kinloop.backend.entity.ParentProfile;
import com.kinloop.backend.entity.Question;
import com.kinloop.backend.entity.QuestionOption;
import com.kinloop.backend.entity.QuestionnaireSession;
import com.kinloop.backend.entity.User;
import com.kinloop.backend.entity.enums.QuestionScope;
import com.kinloop.backend.entity.enums.QuestionType;
import com.kinloop.backend.entity.enums.SessionStatus;
import com.kinloop.backend.mapper.ProfileMapper;
import com.kinloop.backend.mapper.profile.OptionProfileAnswerValueResolver;
import com.kinloop.backend.repository.ChildAnswerRepository;
import com.kinloop.backend.repository.ChildRepository;
import com.kinloop.backend.repository.ParentProfileRepository;
import com.kinloop.backend.repository.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock private ParentProfileRepository parentProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChildRepository childRepository;
    @Mock private ChildAnswerRepository childAnswerRepository;
    @Mock private ChildService childService;

    private ProfileService service;

    @BeforeEach
    void setUp() {
        service = new ProfileService(
                parentProfileRepository,
                userRepository,
                childRepository,
                childAnswerRepository,
                childService,
                new ProfileMapper(List.of(new OptionProfileAnswerValueResolver())));
    }

    @Test
    void profileContainsEveryChildAndOnlyTheLatestCompletedAnswerPerQuestion() {
        ParentProfile parent = new ParentProfile();
        parent.setId(10L);
        parent.setUserId(20L);
        parent.setDailyTimeBudgetMinutes((short) 20);
        User user = new User();
        user.setId(20L);
        user.setEmail("parent@example.com");
        Child deniz = child(101L, "Deniz", 60);
        Child ece = child(102L, "Ece", 36);

        Question question = question(1L, "Q2", 1);
        ChildAnswer newest = answer(deniz.getId(), question, option(question, "3", "Hareket ve müzik"));
        ChildAnswer older = answer(deniz.getId(), question, option(question, "1", "Boyama"));
        ReflectionTestUtils.setField(newest, "answeredAt", OffsetDateTime.now());
        ReflectionTestUtils.setField(older, "answeredAt", OffsetDateTime.now().minusDays(1));

        when(parentProfileRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(childRepository.findByParentIdAndDeletedAtIsNullOrderByIdAsc(10L))
                .thenReturn(List.of(deniz, ece));
        when(childAnswerRepository.findByChildIdsAndSessionStatus(
                List.of(101L, 102L), SessionStatus.COMPLETED)).thenReturn(List.of(newest, older));
        when(childService.displayName(org.mockito.ArgumentMatchers.any(Child.class), anyInt()))
                .thenAnswer(invocation -> invocation.<Child>getArgument(0).getFullName());

        ProfileResponse response = service.getProfile(10L);

        assertEquals("parent@example.com", response.parent().email());
        assertEquals((short) 20, response.parent().dailyTimeBudgetMinutes());
        assertEquals(2, response.children().size());
        assertEquals("Deniz", response.children().getFirst().displayName());
        assertEquals(1, response.children().getFirst().onboardingAnswers().size());
        assertEquals("3", response.children().getFirst().onboardingAnswers().getFirst().optionCode());
        assertEquals("Hareket ve müzik", response.children().getFirst().onboardingAnswers().getFirst().displayValue());
        assertEquals(0, response.children().get(1).onboardingAnswers().size());
    }

    private Child child(Long id, String name, int ageMonths) {
        Child child = new Child();
        child.setId(id);
        child.setParentId(10L);
        child.setFullName(name);
        child.setBirthDate(LocalDate.now().minusMonths(ageMonths));
        return child;
    }

    private Question question(Long id, String code, int order) {
        Question question = new Question();
        question.setId(id);
        question.setCode(code);
        question.setBody("En çok hangi etkinliklerden hoşlanıyor?");
        question.setQuestionType(QuestionType.SINGLE_CHOICE);
        question.setScope(QuestionScope.CHILD);
        question.setDisplayOrder(order);
        return question;
    }

    private QuestionOption option(Question question, String code, String label) {
        QuestionOption option = new QuestionOption();
        option.setQuestion(question);
        option.setCode(code);
        option.setLabel(label);
        return option;
    }

    private ChildAnswer answer(Long childId, Question question, QuestionOption option) {
        QuestionnaireSession session = new QuestionnaireSession();
        session.setChildId(childId);
        session.setStatus(SessionStatus.COMPLETED);
        return ChildAnswer.ofOption(session, question, option);
    }
}
