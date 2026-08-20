package com.kinloop.backend.dto.profile;

import com.kinloop.backend.entity.enums.QuestionType;
import java.time.OffsetDateTime;

public record ProfileAnswerResponse(
        String questionCode,
        String question,
        QuestionType questionType,
        int displayOrder,
        String optionCode,
        String value,
        String displayValue,
        OffsetDateTime answeredAt
) {
}
