package com.kinloop.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinloop.backend.dto.matching.DailyActivityResponse;
import com.kinloop.backend.dto.matching.DailyPlanResponse;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.exception.CustomAccessDeniedHandler;
import com.kinloop.backend.security.CustomAuthenticationEntryPoint;
import com.kinloop.backend.security.JwtService;
import com.kinloop.backend.service.ActivityMatchingService;
import com.kinloop.backend.service.ChildService;
import com.kinloop.backend.service.CurrentParentProfileService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DailyPlanController.class)
class DailyPlanControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CurrentParentProfileService currentParentProfileService;
    @MockBean private ChildService childService;
    @MockBean private ActivityMatchingService matchingService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private CustomAuthenticationEntryPoint authenticationEntryPoint;
    @MockBean private CustomAccessDeniedHandler accessDeniedHandler;

    @Test
    void todayExposesV6PlanBookkeepingAndItemFlags() throws Exception {
        DailyActivityResponse activity = new DailyActivityResponse(
                21L, "Build a tower", "Use blocks", (short) 12, "DEVELOP", BigDecimal.TEN,
                "Start together", "Practice planning", "Supports sequencing",
                "Use fewer blocks", "Add a constraint", "Notice persistence",
                false, true, false
        );
        DailyPlanResponse response = new DailyPlanResponse(
                7L, 9L, LocalDate.of(2026, 8, 22), 25, 35, 24, 36, (short) 2, List.of(activity)
        );
        when(currentParentProfileService.currentParentProfileId(any())).thenReturn(5L);
        when(childService.getOwnedChild(9L, 5L)).thenReturn(new Child());
        when(matchingService.today(any())).thenReturn(response);

        mockMvc.perform(get("/api/children/9/daily-plan/today")
                        .with(user("parent@example.com").roles("PARENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetMin").value(25))
                .andExpect(jsonPath("$.budgetMax").value(35))
                .andExpect(jsonPath("$.committedDurationMinutes").value(24))
                .andExpect(jsonPath("$.totalDurationMinutes").value(36))
                .andExpect(jsonPath("$.fallbackLevel").value(2))
                .andExpect(jsonPath("$.activities[0].withinBudget").value(false))
                .andExpect(jsonPath("$.activities[0].repeatNotice").value(true));
    }
}
