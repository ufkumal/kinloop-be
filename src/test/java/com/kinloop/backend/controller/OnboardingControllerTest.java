package com.kinloop.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinloop.backend.dto.onboarding.DailyTimeBudgetResponse;
import com.kinloop.backend.dto.onboarding.OnboardingClosingMessageResponse;
import com.kinloop.backend.dto.profile.DailyTimeBudgetOptionResponse;
import com.kinloop.backend.dto.profile.DailyTimeBudgetProfileResponse;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.enums.OnboardingClosingAction;
import com.kinloop.backend.exception.CustomAccessDeniedHandler;
import com.kinloop.backend.security.CustomAuthenticationEntryPoint;
import com.kinloop.backend.security.JwtService;
import com.kinloop.backend.service.ChildService;
import com.kinloop.backend.service.CurrentParentProfileService;
import com.kinloop.backend.service.OnboardingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OnboardingController.class)
class OnboardingControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private OnboardingService onboardingService;
    @MockBean private CurrentParentProfileService currentParentProfileService;
    @MockBean private ChildService childService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private CustomAuthenticationEntryPoint authenticationEntryPoint;
    @MockBean private CustomAccessDeniedHandler accessDeniedHandler;

    private Child child;

    @BeforeEach
    void setUp() {
        child = new Child();
        child.setId(9L);
        when(currentParentProfileService.currentParentProfileId(any())).thenReturn(5L);
        when(childService.getOwnedChild(9L, 5L)).thenReturn(child);
    }

    @Test
    void budgetEndpointReturnsAgeFilteredRangesForTheOwnedChild() throws Exception {
        when(onboardingService.getDailyTimeBudget(child)).thenReturn(new DailyTimeBudgetProfileResponse(
                "Q7", "Question", "B", (short) 25, (short) 35,
                List.of(
                        new DailyTimeBudgetOptionResponse("A", "Short", 1, (short) 15, (short) 25),
                        new DailyTimeBudgetOptionResponse("B", "Half hour", 2, (short) 25, (short) 35))));

        mockMvc.perform(get("/api/children/9/onboarding/daily-time-budget")
                        .with(user("parent@example.com").roles("PARENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minMinutes").value(25))
                .andExpect(jsonPath("$.maxMinutes").value(35))
                .andExpect(jsonPath("$.options.length()").value(2));

        verify(childService).getOwnedChild(9L, 5L);
    }

    @Test
    void budgetUpdateTargetsTheChildInsteadOfTheHousehold() throws Exception {
        when(onboardingService.updateDailyTimeBudget(child, "A"))
                .thenReturn(new DailyTimeBudgetResponse("A", (short) 15, (short) 25));

        mockMvc.perform(put("/api/children/9/onboarding/daily-time-budget")
                        .with(user("parent@example.com").roles("PARENT"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"optionCode\":\"A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answeredOptionCode").value("A"))
                .andExpect(jsonPath("$.minMinutes").value(15))
                .andExpect(jsonPath("$.maxMinutes").value(25));
    }

    @Test
    void closingMessageAcceptsRemindLaterAndReturnsPlanReminderState() throws Exception {
        when(onboardingService.respondToClosingMessage(child, OnboardingClosingAction.REMIND_LATER))
                .thenReturn(new OnboardingClosingMessageResponse(false, true, 3));

        mockMvc.perform(put("/api/children/9/onboarding/closing-message")
                        .with(user("parent@example.com").roles("PARENT"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"action\":\"REMIND_LATER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shouldDisplay").value(false))
                .andExpect(jsonPath("$.planReminderEnabled").value(true))
                .andExpect(jsonPath("$.reminderPlansRemaining").value(3));
    }
}
