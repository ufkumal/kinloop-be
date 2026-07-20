package com.kinloop.backend.dto.child;

import com.kinloop.backend.entity.enums.SessionStatus;
import com.kinloop.backend.entity.enums.TriggerReason;

public record SessionSummaryResponse(
        Long sessionId,
        SessionStatus status,
        TriggerReason triggerReason,
        int totalQuestions,
        int answeredCount,
        String nextQuestionCode
) {
}
