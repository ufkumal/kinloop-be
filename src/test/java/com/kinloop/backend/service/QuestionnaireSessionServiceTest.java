package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinloop.backend.dto.questionnaire.CurrentQuestionnaireResponse;
import com.kinloop.backend.entity.ChildAnswer;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.ChildProfileSnapshot;
import com.kinloop.backend.entity.Question;
import com.kinloop.backend.entity.QuestionnaireSession;
import com.kinloop.backend.entity.enums.AgeBand;
import com.kinloop.backend.entity.enums.SessionStatus;
import com.kinloop.backend.entity.enums.TriggerReason;
import com.kinloop.backend.mapper.QuestionnaireMapper;
import com.kinloop.backend.repository.ChildAnswerRepository;
import com.kinloop.backend.repository.ChildRepository;
import com.kinloop.backend.repository.QuestionOptionRepository;
import com.kinloop.backend.repository.QuestionRepository;
import com.kinloop.backend.repository.QuestionnaireSessionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionnaireSessionServiceTest {

    @Mock private QuestionnaireSessionRepository sessionRepository;
    @Mock private ChildRepository childRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionOptionRepository optionRepository;
    @Mock private ChildAnswerRepository answerRepository;
    @Mock private ProfileSnapshotService snapshotService;
    @Mock private QuestionnaireMapper mapper;
    @Mock private QuestionnaireSessionWriter sessionWriter;
    @Mock private Question completedQuestion;
    @Mock private ChildAnswer completedAnswer;

    private QuestionnaireSessionService service;

    @BeforeEach
    void setUp() {
        service = new QuestionnaireSessionService(sessionRepository, childRepository, questionRepository,
                optionRepository, answerRepository, snapshotService, mapper, sessionWriter);
    }

    @Test
    void completionRecordsChildOnboardingTimestamp() {
        Child child = new Child();
        child.setId(10L);
        child.setParentId(30L);
        child.setBirthDate(LocalDate.now().minusMonths(18));

        QuestionnaireSession session = QuestionnaireSession.open(10L, 18, TriggerReason.INITIAL);
        session.setId(20L);
        when(sessionRepository.findByChildIdAndStatus(10L, SessionStatus.IN_PROGRESS))
                .thenReturn(Optional.of(session));
        when(questionRepository.findForAge(18, com.kinloop.backend.entity.enums.QuestionScope.CHILD))
                .thenReturn(List.of());
        when(answerRepository.findBySessionId(20L)).thenReturn(List.of());
        when(snapshotService.rebuild(session)).thenReturn(new ChildProfileSnapshot());

        service.complete(child);

        assertEquals(SessionStatus.COMPLETED, session.getStatus());
        assertNotNull(child.getOnboardingCompletedAt());
        assertEquals(session.getCompletedAt(), child.getOnboardingCompletedAt());
        assertEquals(AgeBand.BAND_12_24, session.getAgeBand());
        verify(childRepository).save(child);
    }

    @Test
    void currentReturnsCompletedSessionWhenCurrentAgeBandIsAlreadyCompleted() {
        Child child = childAtAgeMonths(18);
        QuestionnaireSession completed = QuestionnaireSession.open(10L, 18, TriggerReason.INITIAL);
        completed.setId(20L);
        completed.markCompleted();
        List<Question> questions = List.of(completedQuestion);
        List<ChildAnswer> answers = List.of(completedAnswer);
        CurrentQuestionnaireResponse expected = new CurrentQuestionnaireResponse(
                20L, SessionStatus.COMPLETED, TriggerReason.INITIAL, AgeBand.BAND_12_24,
                18, 1, 1, null, List.of());

        when(sessionRepository.findByChildIdAndStatus(10L, SessionStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(sessionRepository.findFirstByChildIdAndStatusAndAgeBandOrderByCompletedAtDesc(
                10L, SessionStatus.COMPLETED, AgeBand.BAND_12_24)).thenReturn(Optional.of(completed));
        when(questionRepository.findForAge(18, com.kinloop.backend.entity.enums.QuestionScope.CHILD))
                .thenReturn(questions);
        when(answerRepository.findBySessionId(20L)).thenReturn(answers);
        when(mapper.toCurrent(completed, questions, answers)).thenReturn(expected);

        CurrentQuestionnaireResponse actual = service.current(child);

        assertSame(expected, actual);
        verify(sessionWriter, never()).insert(any());
        verify(sessionRepository, never()).saveAndFlush(any(QuestionnaireSession.class));
    }

    @Test
    void currentOpensAgeBandSessionWhenOnlyCompletedSessionBelongsToOlderBand() {
        Child child = childAtAgeMonths(25);
        when(sessionRepository.findByChildIdAndStatus(10L, SessionStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(sessionRepository.findFirstByChildIdAndStatusAndAgeBandOrderByCompletedAtDesc(
                10L, SessionStatus.COMPLETED, AgeBand.BAND_24_48)).thenReturn(Optional.empty());
        when(sessionRepository.existsByChildIdAndStatus(10L, SessionStatus.COMPLETED)).thenReturn(true);
        when(sessionWriter.insert(any(QuestionnaireSession.class))).thenAnswer(invocation -> {
            QuestionnaireSession session = invocation.getArgument(0);
            session.setId(30L);
            return session;
        });
        when(questionRepository.findForAge(25, com.kinloop.backend.entity.enums.QuestionScope.CHILD))
                .thenReturn(List.of());
        when(answerRepository.findBySessionId(30L)).thenReturn(List.of());

        service.current(child);

        ArgumentCaptor<QuestionnaireSession> sessionCaptor = ArgumentCaptor.forClass(QuestionnaireSession.class);
        verify(sessionWriter).insert(sessionCaptor.capture());
        assertEquals(SessionStatus.IN_PROGRESS, sessionCaptor.getValue().getStatus());
        assertEquals(TriggerReason.AGE_BAND, sessionCaptor.getValue().getTriggerReason());
        assertEquals(AgeBand.BAND_24_48, sessionCaptor.getValue().getAgeBand());
    }

    private Child childAtAgeMonths(int ageMonths) {
        Child child = new Child();
        child.setId(10L);
        child.setParentId(30L);
        child.setBirthDate(LocalDate.now().minusMonths(ageMonths));
        return child;
    }
}
