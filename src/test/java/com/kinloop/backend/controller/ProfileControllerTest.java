package com.kinloop.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinloop.backend.exception.CustomAccessDeniedHandler;
import com.kinloop.backend.security.CustomAuthenticationEntryPoint;
import com.kinloop.backend.security.JwtService;
import com.kinloop.backend.service.CurrentParentProfileService;
import com.kinloop.backend.service.ProfileService;
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
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private CustomAuthenticationEntryPoint authenticationEntryPoint;
    @MockBean private CustomAccessDeniedHandler accessDeniedHandler;

    @Test
    void authenticatedParentCanOpenProfile() throws Exception {
        org.mockito.Mockito.when(currentParentProfileService.currentParentProfileId(any())).thenReturn(21L);

        mockMvc.perform(get("/api/profile")
                        .with(user("parent@example.com").roles("PARENT")))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }
}
