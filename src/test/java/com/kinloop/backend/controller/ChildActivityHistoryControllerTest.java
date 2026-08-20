package com.kinloop.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinloop.backend.dto.profile.ActivityHistoryResponse;
import com.kinloop.backend.exception.CustomAccessDeniedHandler;
import com.kinloop.backend.security.CustomAuthenticationEntryPoint;
import com.kinloop.backend.security.JwtService;
import com.kinloop.backend.service.ActivityHistoryService;
import com.kinloop.backend.service.ChildService;
import com.kinloop.backend.service.CurrentParentProfileService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChildActivityHistoryController.class)
class ChildActivityHistoryControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CurrentParentProfileService currentParentProfileService;
    @MockBean private ChildService childService;
    @MockBean private ActivityHistoryService activityHistoryService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private CustomAuthenticationEntryPoint authenticationEntryPoint;
    @MockBean private CustomAccessDeniedHandler accessDeniedHandler;

    @Test
    void historyIsLoadedOnlyAfterOwnershipCheck() throws Exception {
        when(currentParentProfileService.currentParentProfileId(any())).thenReturn(5L);
        when(activityHistoryService.getSelectedHistory(9L, 0, 20))
                .thenReturn(new ActivityHistoryResponse(9L, 0, 20, false, List.of()));

        mockMvc.perform(get("/api/children/9/activity-history")
                        .with(user("parent@example.com").roles("PARENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.childId").value(9))
                .andExpect(jsonPath("$.activities").isEmpty());

        verify(childService).getOwnedChild(9L, 5L);
        verify(activityHistoryService).getSelectedHistory(9L, 0, 20);
    }

    @Test
    void pageSizeIsBounded() throws Exception {
        mockMvc.perform(get("/api/children/9/activity-history?size=101")
                        .with(user("parent@example.com").roles("PARENT")))
                .andExpect(status().isBadRequest());
    }
}
