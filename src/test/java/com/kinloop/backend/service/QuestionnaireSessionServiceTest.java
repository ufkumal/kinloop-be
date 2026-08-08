package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.ChildProfileSnapshot;
import com.kinloop.backend.entity.QuestionnaireSession;
import com.kinloop.backend.entity.ParentProfile;
import com.kinloop.backend.entity.enums.AgeBand;
import com.kinloop.backend.entity.enums.SessionStatus;
import com.kinloop.backend.entity.enums.TriggerReason;
import com.kinloop.backend.mapper.QuestionnaireMapper;
import com.kinloop.backend.repository.ChildAnswerRepository;
import com.kinloop.backend.repository.ChildRepository;
import com.kinloop.backend.repository.QuestionOptionRepository;
import com.kinloop.backend.repository.QuestionRepository;
import com.kinloop.backend.repository.QuestionnaireSessionRepository;
import com.kinloop.backend.repository.ParentProfileRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
    @Mock private ParentProfileRepository parentProfileRepository;

    private QuestionnaireSessionService service;

    @BeforeEach
    void setUp() {
        service = new QuestionnaireSessionService(sessionRepository, childRepository, questionRepository,
                optionRepository, answerRepository, snapshotService, mapper, sessionWriter, parentProfileRepository);
    }

    @Test
    void completionRecordsChildOnboardingTimestamp() {
        Child child = new Child();
        child.setId(10L);
        child.setParentId(30L);
        child.setBirthDate(LocalDate.now().minusMonths(18));

        ParentProfile parentProfile = new ParentProfile();
        parentProfile.setId(30L);
        parentProfile.setDailyTimeBudgetMinutes((short) 20);

        QuestionnaireSession session = QuestionnaireSession.open(10L, 18, TriggerReason.INITIAL);
        session.setId(20L);
        when(sessionRepository.findByChildIdAndStatus(10L, SessionStatus.IN_PROGRESS))
                .thenReturn(Optional.of(session));
        when(parentProfileRepository.findById(30L)).thenReturn(Optional.of(parentProfile));
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
}
