package com.kinloop.backend.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinloop.backend.dto.auth.RegisterRequest;
import com.kinloop.backend.entity.ParentProfile;
import com.kinloop.backend.entity.User;
import com.kinloop.backend.entity.WorkshopProfile;
import com.kinloop.backend.entity.enums.UserRole;
import com.kinloop.backend.repository.EmailVerificationTokenRepository;
import com.kinloop.backend.repository.ParentProfileRepository;
import com.kinloop.backend.repository.UserRepository;
import com.kinloop.backend.repository.WorkshopProfileRepository;
import com.kinloop.backend.security.JwtService;
import com.kinloop.backend.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailVerificationTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;
    @Mock private ParentProfileRepository parentProfileRepository;
    @Mock private WorkshopProfileRepository workshopProfileRepository;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, tokenRepository, passwordEncoder, jwtService,
                emailService, parentProfileRepository, workshopProfileRepository);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });
    }

    @Test
    void parentRegistrationCreatesOnlyParentProfile() {
        authService.register(new RegisterRequest("Parent Name", "parent@example.com", "password", UserRole.PARENT));

        ArgumentCaptor<ParentProfile> profile = ArgumentCaptor.forClass(ParentProfile.class);
        verify(parentProfileRepository).save(profile.capture());
        verify(workshopProfileRepository, never()).save(any());
        org.junit.jupiter.api.Assertions.assertEquals(42L, profile.getValue().getUserId());
        org.junit.jupiter.api.Assertions.assertEquals("Parent Name", profile.getValue().getFullName());
    }

    @Test
    void workshopRegistrationCreatesOnlyWorkshopProfile() {
        authService.register(new RegisterRequest("Workshop Name", "workshop@example.com", "password", UserRole.WORKSHOP));

        ArgumentCaptor<WorkshopProfile> profile = ArgumentCaptor.forClass(WorkshopProfile.class);
        verify(workshopProfileRepository).save(profile.capture());
        verify(parentProfileRepository, never()).save(any());
        org.junit.jupiter.api.Assertions.assertEquals(42L, profile.getValue().getUserId());
        org.junit.jupiter.api.Assertions.assertEquals("Workshop Name", profile.getValue().getName());
    }
}
