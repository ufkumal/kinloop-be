package com.kinloop.backend.dto.consent;

import com.kinloop.backend.entity.enums.ConsentType;
import java.time.OffsetDateTime;

public record ConsentResponse(
        Long consentId,
        ConsentType type,
        String version,
        String title,
        String summary,
        String content,
        boolean required,
        OffsetDateTime effectiveAt,
        Boolean granted,
        OffsetDateTime respondedAt
) {}
