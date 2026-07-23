package com.kinloop.backend.dto.questionnaire;
import com.kinloop.backend.entity.enums.*;
import java.util.List;
public record CurrentQuestionnaireResponse(Long sessionId, SessionStatus status, TriggerReason triggerReason,
        AgeBand ageBand, int ageMonths, int totalQuestions, int answeredCount, String nextQuestionCode,
        List<QuestionnaireQuestionResponse> questions) { }
