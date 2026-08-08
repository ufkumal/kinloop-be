package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinloop.backend.dto.home.HomeStatusResponse;
import com.kinloop.backend.repository.ChildRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock private ChildRepository childRepository;

    private HomeService service;

    @BeforeEach
    void setUp() {
        service = new HomeService(childRepository);
    }

    @Test
    void parentWithoutChildReceivesNewUser() {
        when(childRepository.existsByParentIdAndOnboardingCompletedAtIsNotNullAndDeletedAtIsNull(1L))
                .thenReturn(false);

        HomeStatusResponse response = service.getStatus(1L);

        assertEquals("new-user", response.state());
    }

    @Test
    void parentWithIncompleteOnboardingReceivesNewUser() {
        when(childRepository.existsByParentIdAndOnboardingCompletedAtIsNotNullAndDeletedAtIsNull(1L))
                .thenReturn(false);

        HomeStatusResponse response = service.getStatus(1L);

        assertEquals("new-user", response.state());
    }

    @Test
    void parentWithCompletedOnboardingReceivesReturningUser() {
        when(childRepository.existsByParentIdAndOnboardingCompletedAtIsNotNullAndDeletedAtIsNull(1L))
                .thenReturn(true);

        HomeStatusResponse response = service.getStatus(1L);

        assertEquals("returning-user", response.state());
    }

    @Test
    void statusIsScopedToAuthenticatedParentProfile() {
        when(childRepository.existsByParentIdAndOnboardingCompletedAtIsNotNullAndDeletedAtIsNull(1L))
                .thenReturn(false);
        when(childRepository.existsByParentIdAndOnboardingCompletedAtIsNotNullAndDeletedAtIsNull(2L))
                .thenReturn(true);

        assertEquals("new-user", service.getStatus(1L).state());
        assertEquals("returning-user", service.getStatus(2L).state());

        verify(childRepository).existsByParentIdAndOnboardingCompletedAtIsNotNullAndDeletedAtIsNull(1L);
        verify(childRepository).existsByParentIdAndOnboardingCompletedAtIsNotNullAndDeletedAtIsNull(2L);
    }
}
