package com.kinloop.backend.controller;

import com.kinloop.backend.dto.feedback.FeedbackQuestionsResponse;
import com.kinloop.backend.service.FeedbackQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home/feedback")
@RequiredArgsConstructor
public class HomeFeedbackController {

    private final FeedbackQuestionService feedbackQuestionService;

    @GetMapping("/questions")
    public FeedbackQuestionsResponse questions() {
        return feedbackQuestionService.questions();
    }
}
