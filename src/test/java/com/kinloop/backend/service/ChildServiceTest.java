package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinloop.backend.dto.child.CreateChildRequest;
import com.kinloop.backend.dto.child.SessionSummaryResponse;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.ParentProfile;
import com.kinloop.backend.entity.QuestionnaireSession;
import com.kinloop.backend.entity.enums.Gender;
import com.kinloop.backend.entity.enums.SessionStatus;
import com.kinloop.backend.entity.enums.TriggerReason;
import com.kinloop.backend.exception.MissingDailyTimeBudgetException;
import com.kinloop.backend.repository.ChildRepository;
import com.kinloop.backend.repository.ParentProfileRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChildServiceTest {

    @Mock private ChildRepository childRepository;
    @Mock private ParentProfileRepository parentProfileRepository;
    @Mock private QuestionnaireSessionService questionnaireSessionService;
    @Mock private OnboardingService onboardingService;

    private ChildService service;

    @BeforeEach
    void setUp() {
        service = new ChildService(childRepository, parentProfileRepository, questionnaireSessionService, onboardingService);
    }

    @Test
    void firstChildStoresRequiredDailyTimeBudgetOnParent() {
        ParentProfile parent = parent(1L, null);
        when(parentProfileRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(onboardingService.resolveDailyTimeBudget("B")).thenReturn((short) 20);
        when(childRepository.save(any(Child.class))).thenAnswer(invocation -> {
            Child child = invocation.getArgument(0);
            child.setId(2L);
            return child;
        });
        QuestionnaireSession session = QuestionnaireSession.open(2L, 18, TriggerReason.INITIAL);
        when(questionnaireSessionService.openInitialSession(2L, 18)).thenReturn(session);
        when(questionnaireSessionService.summarise(session, 0))
                .thenReturn(new SessionSummaryResponse(null, SessionStatus.IN_PROGRESS, TriggerReason.INITIAL, 3, 0, "Q2"));

        service.createChild(1L, new CreateChildRequest("Ada", LocalDate.now().minusMonths(18), Gender.FEMALE, "B"));

        assertEquals(Short.valueOf((short) 20), parent.getDailyTimeBudgetMinutes());
        verify(parentProfileRepository).save(parent);
    }

    @Test
    void childCreationFailsBeforeWritingWhenParentHasNoBudgetAnswer() {
        ParentProfile parent = parent(1L, null);
        when(parentProfileRepository.findById(1L)).thenReturn(Optional.of(parent));

        assertThrows(MissingDailyTimeBudgetException.class,
                () -> service.createChild(1L, new CreateChildRequest("Ada", LocalDate.now().minusMonths(18), Gender.FEMALE, null)));

        verify(childRepository, never()).save(any());
    }

    @Test
    void subsequentChildKeepsExistingParentBudget() {
        ParentProfile parent = parent(1L, (short) 10);
        when(parentProfileRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(childRepository.save(any(Child.class))).thenAnswer(invocation -> {
            Child child = invocation.getArgument(0);
            child.setId(3L);
            return child;
        });
        QuestionnaireSession session = QuestionnaireSession.open(3L, 18, TriggerReason.INITIAL);
        when(questionnaireSessionService.openInitialSession(3L, 18)).thenReturn(session);
        when(questionnaireSessionService.summarise(session, 0))
                .thenReturn(new SessionSummaryResponse(null, SessionStatus.IN_PROGRESS, TriggerReason.INITIAL, 3, 0, "Q2"));

        service.createChild(1L, new CreateChildRequest("Ece", LocalDate.now().minusMonths(18), Gender.FEMALE, null));

        assertEquals(Short.valueOf((short) 10), parent.getDailyTimeBudgetMinutes());
        verify(parentProfileRepository, never()).save(any());
    }

    private ParentProfile parent(Long id, Short budget) {
        ParentProfile parent = new ParentProfile();
        parent.setId(id);
        parent.setDailyTimeBudgetMinutes(budget);
        return parent;
    }
}
