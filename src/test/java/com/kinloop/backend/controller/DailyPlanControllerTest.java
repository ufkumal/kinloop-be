package com.kinloop.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinloop.backend.dto.matching.DailyActivityResponse;
import com.kinloop.backend.dto.matching.DailyPlanResponse;
import com.kinloop.backend.dto.matching.ActivityMaterialResponse;
import com.kinloop.backend.dto.matching.ActivityOutcomeResponse;
import com.kinloop.backend.dto.matching.ActivityStepResponse;
import com.kinloop.backend.dto.feedback.ActivityFeedbackResponse;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.FeedbackType;
import com.kinloop.backend.entity.enums.IntelligenceType;
import com.kinloop.backend.entity.enums.InvolvementType;
import com.kinloop.backend.exception.CustomAccessDeniedHandler;
import com.kinloop.backend.security.CustomAuthenticationEntryPoint;
import com.kinloop.backend.security.JwtService;
import com.kinloop.backend.service.ActivityMatchingService;
import com.kinloop.backend.service.ChildService;
import com.kinloop.backend.service.CurrentParentProfileService;
import com.kinloop.backend.service.SynchronousFeedbackSubmissionService;
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
    @MockBean private SynchronousFeedbackSubmissionService feedbackSubmissionService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private CustomAuthenticationEntryPoint authenticationEntryPoint;
    @MockBean private CustomAccessDeniedHandler accessDeniedHandler;

    @Test
    void todayExposesV6PlanBookkeepingAndItemFlags() throws Exception {
        DailyActivityResponse activity = new DailyActivityResponse(
                56L, 21L, "Build a tower", "Use blocks", 24, 48,
                IntelligenceType.LOGICAL_MATHEMATICAL, IntelligenceType.VISUAL_SPATIAL,
                DevelopmentDomain.COGNITIVE, (short) 2, (short) 12, InvolvementType.BIRLIKTE,
                (short) 1, (short) 2, (short) 2, "DEVELOP", BigDecimal.TEN,
                "Start together", "Practice planning", "Supports sequencing",
                "Use fewer blocks", "Add a constraint", "Notice persistence",
                "Use large blocks", "Put blocks away",
                List.of(new ActivityStepResponse((short) 1, "Stack two blocks")),
                List.of(new ActivityMaterialResponse(
                        "Blocks", "TOY", "6", false, 1, "Prefer large blocks")),
                List.of(new ActivityOutcomeResponse((short) 1, "Practices planning")),
                false, true, false, false, null, null
        );
        DailyPlanResponse response = new DailyPlanResponse(
                7L, 9L, LocalDate.of(2026, 8, 22), 25, 35, 24, 36, (short) 2,
                List.of(activity), "READY", null, true
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
                .andExpect(jsonPath("$.state").value("READY"))
                .andExpect(jsonPath("$.showOnboardingReminder").value(true))
                .andExpect(jsonPath("$.activities[0].dailyPlanItemId").value(56))
                .andExpect(jsonPath("$.activities[0].safetyNotes").value("Use large blocks"))
                .andExpect(jsonPath("$.activities[0].steps[0].text").value("Stack two blocks"))
                .andExpect(jsonPath("$.activities[0].materials[0].name").value("Blocks"))
                .andExpect(jsonPath("$.activities[0].outcomes[0].outcome").value("Practices planning"))
                .andExpect(jsonPath("$.activities[0].withinBudget").value(false))
                .andExpect(jsonPath("$.activities[0].repeatNotice").value(true));
    }

    @Test
    void feedbackEndpointChecksOwnershipAndReturnsLearningState() throws Exception {
        Child child = new Child();
        child.setId(9L);
        when(currentParentProfileService.currentParentProfileId(any())).thenReturn(5L);
        when(childService.getOwnedChild(9L, 5L)).thenReturn(child);
        when(feedbackSubmissionService.submit(any(), org.mockito.ArgumentMatchers.eq(11L), any()))
                .thenReturn(new ActivityFeedbackResponse(
                        31L, 11L, FeedbackType.LIKED, null,
                        DevelopmentDomain.LANGUAGE, (short) 2, new BigDecimal("0.5")));

        mockMvc.perform(post("/api/children/9/daily-plan/items/11/feedback")
                        .with(user("parent@example.com").roles("PARENT"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"feedbackType\":\"LIKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedbackType").value("LIKED"))
                .andExpect(jsonPath("$.domain").value("LANGUAGE"))
                .andExpect(jsonPath("$.domainLevel").value(2))
                .andExpect(jsonPath("$.domainStreak").value(0.5));
    }
}
