package com.kinloop.backend.dto.child;

import com.kinloop.backend.entity.enums.AgeBand;
import java.time.LocalDate;

public record CreateChildResponse(
        Long childId,
        String fullName,
        String displayName,
        LocalDate birthDate,
        int ageMonths,
        AgeBand ageBand,
        SessionSummaryResponse session
) {
}
