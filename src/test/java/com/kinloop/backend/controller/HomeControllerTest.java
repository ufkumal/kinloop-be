package com.kinloop.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinloop.backend.dto.home.HomeStatusResponse;
import com.kinloop.backend.exception.CustomAccessDeniedHandler;
import com.kinloop.backend.security.CustomAuthenticationEntryPoint;
import com.kinloop.backend.security.JwtService;
import com.kinloop.backend.service.CurrentParentProfileService;
import com.kinloop.backend.service.HomeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private HomeService homeService;
    @MockBean private CurrentParentProfileService currentParentProfileService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private CustomAuthenticationEntryPoint authenticationEntryPoint;
    @MockBean private CustomAccessDeniedHandler accessDeniedHandler;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/home/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedParentReceivesExactNewUserResponse() throws Exception {
        when(currentParentProfileService.currentParentProfileId(any())).thenReturn(11L);
        when(homeService.getStatus(11L)).thenReturn(HomeStatusResponse.newUser());

        mockMvc.perform(get("/api/home/status").with(user("parent@example.com").roles("PARENT")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"state\":\"new-user\"}", true));
    }

    @Test
    void authenticatedParentReceivesExactReturningUserResponse() throws Exception {
        when(currentParentProfileService.currentParentProfileId(any())).thenReturn(22L);
        when(homeService.getStatus(22L)).thenReturn(HomeStatusResponse.returningUser());

        mockMvc.perform(get("/api/home/status").with(user("parent@example.com").roles("PARENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("returning-user"));

        verify(homeService).getStatus(22L);
    }
}
