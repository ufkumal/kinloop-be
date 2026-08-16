package com.kinloop.backend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinloop.backend.dto.feedback.FeedbackQuestionResponse;
import com.kinloop.backend.dto.feedback.FeedbackQuestionsResponse;
import com.kinloop.backend.dto.questionnaire.QuestionOptionResponse;
import com.kinloop.backend.entity.enums.QuestionType;
import com.kinloop.backend.exception.CustomAccessDeniedHandler;
import com.kinloop.backend.security.CustomAuthenticationEntryPoint;
import com.kinloop.backend.security.JwtService;
import com.kinloop.backend.service.FeedbackQuestionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomeFeedbackController.class)
class HomeFeedbackControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private FeedbackQuestionService feedbackQuestionService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private CustomAuthenticationEntryPoint authenticationEntryPoint;
    @MockBean private CustomAccessDeniedHandler accessDeniedHandler;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/home/feedback/questions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsDatabaseBackedFeedbackQuestions() throws Exception {
        FeedbackQuestionResponse question = new FeedbackQuestionResponse(
                "FB_ENJOYMENT",
                "Çocuğun etkinlikten keyif aldı mı?",
                null,
                QuestionType.SINGLE_CHOICE,
                true,
                1,
                null,
                List.of(new QuestionOptionResponse("LOVED", "Çok sevdi", 1))
        );
        when(feedbackQuestionService.questions())
                .thenReturn(new FeedbackQuestionsResponse(List.of(question)));

        mockMvc.perform(get("/api/home/feedback/questions")
                        .with(user("parent@example.com").roles("PARENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].code").value("FB_ENJOYMENT"))
                .andExpect(jsonPath("$.questions[0].type").value("SINGLE_CHOICE"))
                .andExpect(jsonPath("$.questions[0].options[0].code").value("LOVED"));
    }
}
