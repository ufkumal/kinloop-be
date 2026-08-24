package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinloop.backend.dto.child.CreateChildRequest;
import com.kinloop.backend.dto.child.SessionSummaryResponse;
import com.kinloop.backend.dto.child.UpdateChildRequest;
import com.kinloop.backend.dto.child.UpdateChildResponse;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.ParentProfile;
import com.kinloop.backend.entity.QuestionnaireSession;
import com.kinloop.backend.entity.enums.Gender;
import com.kinloop.backend.entity.enums.SessionStatus;
import com.kinloop.backend.entity.enums.TriggerReason;
import com.kinloop.backend.repository.ChildRepository;
import com.kinloop.backend.repository.ParentProfileRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

    private ChildService service;

    @BeforeEach
    void setUp() {
        service = new ChildService(childRepository, parentProfileRepository, questionnaireSessionService);
    }

    @Test
    void childUsesDefaultDailyTimeBudgetUntilOnboardingUpdatesIt() {
        ParentProfile parent = parent();
        when(parentProfileRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(childRepository.save(any(Child.class))).thenAnswer(invocation -> {
            Child child = invocation.getArgument(0);
            child.setId(2L);
            return child;
        });
        QuestionnaireSession session = QuestionnaireSession.open(2L, 18, TriggerReason.INITIAL);
        when(questionnaireSessionService.openInitialSession(2L, 18)).thenReturn(session);
        when(questionnaireSessionService.summarise(session, 0))
                .thenReturn(new SessionSummaryResponse(null, SessionStatus.IN_PROGRESS, TriggerReason.INITIAL, 3, 0, "Q2"));

        service.createChild(1L, new CreateChildRequest("Ada", LocalDate.now().minusMonths(18), Gender.FEMALE));

        org.mockito.ArgumentCaptor<Child> childCaptor = org.mockito.ArgumentCaptor.forClass(Child.class);
        verify(childRepository).save(childCaptor.capture());
        assertEquals((short) 25, childCaptor.getValue().getDailyTimeBudgetMin());
        assertEquals((short) 35, childCaptor.getValue().getDailyTimeBudgetMax());
        verify(parentProfileRepository, never()).save(any());
    }

    @Test
    void profileCanUpdateOwnedChildIdentityWithinTheSameAgeBand() {
        Child child = child(18);
        when(childRepository.findByIdAndParentIdAndDeletedAtIsNull(2L, 1L)).thenReturn(Optional.of(child));

        UpdateChildResponse response = service.updateChild(
                2L, 1L, new UpdateChildRequest("  Ada Updated  ", LocalDate.now().minusMonths(19), Gender.FEMALE));

        assertEquals("Ada Updated", response.fullName());
        assertFalse(response.questionnaireRestarted());
        verify(childRepository).saveAndFlush(child);
        verify(questionnaireSessionService, never()).current(any());
    }

    @Test
    void ageBandCorrectionRestartsQuestionnaireAndHomeOnboarding() {
        Child child = child(30);
        child.setOnboardingCompletedAt(OffsetDateTime.now());
        when(childRepository.findByIdAndParentIdAndDeletedAtIsNull(2L, 1L)).thenReturn(Optional.of(child));

        UpdateChildResponse response = service.updateChild(
                2L, 1L, new UpdateChildRequest("Ada", LocalDate.now().minusMonths(18), Gender.FEMALE));

        assertTrue(response.questionnaireRestarted());
        assertNull(child.getOnboardingCompletedAt());
        verify(questionnaireSessionService).current(child);
    }

    private ParentProfile parent() {
        ParentProfile parent = new ParentProfile();
        parent.setId(1L);
        return parent;
    }

    private Child child(int ageMonths) {
        Child child = new Child();
        child.setId(2L);
        child.setParentId(1L);
        child.setFullName("Ada");
        child.setBirthDate(LocalDate.now().minusMonths(ageMonths));
        child.setGender(Gender.FEMALE);
        return child;
    }
}
