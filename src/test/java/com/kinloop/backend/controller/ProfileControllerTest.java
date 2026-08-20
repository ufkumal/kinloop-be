package com.kinloop.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinloop.backend.dto.profile.DailyTimeBudgetOptionResponse;
import com.kinloop.backend.dto.profile.DailyTimeBudgetProfileResponse;
import com.kinloop.backend.exception.CustomAccessDeniedHandler;
import com.kinloop.backend.security.CustomAuthenticationEntryPoint;
import com.kinloop.backend.security.JwtService;
import com.kinloop.backend.service.CurrentParentProfileService;
import com.kinloop.backend.service.OnboardingService;
import com.kinloop.backend.service.ProfileService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CurrentParentProfileService currentParentProfileService;
    @MockBean private ProfileService profileService;
    @MockBean private OnboardingService onboardingService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private CustomAuthenticationEntryPoint authenticationEntryPoint;
    @MockBean private CustomAccessDeniedHandler accessDeniedHandler;

    @Test
    void authenticatedParentReceivesDatabaseBackedDailyTimeBudget() throws Exception {
        DailyTimeBudgetProfileResponse response = new DailyTimeBudgetProfileResponse(
                "Q7",
                "Çocuğunuzla günde ne kadar vakit ayırabiliyorsunuz?",
                "B",
                (short) 20,
                List.of(
                        new DailyTimeBudgetOptionResponse("A", "5-10 dk", 1, (short) 10),
                        new DailyTimeBudgetOptionResponse("B", "15-20 dk", 2, (short) 20),
                        new DailyTimeBudgetOptionResponse("C", "30+ dk", 3, (short) 30)));
        when(currentParentProfileService.currentParentProfileId(any())).thenReturn(21L);
        when(onboardingService.getDailyTimeBudget(21L)).thenReturn(response);

        mockMvc.perform(get("/api/profile/daily-time-budget")
                        .with(user("parent@example.com").roles("PARENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionCode").value("Q7"))
                .andExpect(jsonPath("$.selectedOptionCode").value("B"))
                .andExpect(jsonPath("$.dailyTimeBudgetMinutes").value(20))
                .andExpect(jsonPath("$.options[0].minutes").value(10))
                .andExpect(jsonPath("$.options[2].minutes").value(30));

        verify(onboardingService).getDailyTimeBudget(21L);
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/profile/daily-time-budget"))
                .andExpect(status().isUnauthorized());
    }
}
