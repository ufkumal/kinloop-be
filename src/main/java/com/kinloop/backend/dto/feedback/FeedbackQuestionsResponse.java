package com.kinloop.backend.dto.feedback;

import java.util.List;

public record FeedbackQuestionsResponse(List<FeedbackQuestionResponse> questions) {
}
