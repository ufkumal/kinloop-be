package com.kinloop.backend.dto.questionnaire;
import com.kinloop.backend.entity.enums.*;
public record QuestionnaireCompleteResponse(Long sessionId, SessionStatus status, Long snapshotId,
        AgeBand ageBand, int ageMonths) { }
