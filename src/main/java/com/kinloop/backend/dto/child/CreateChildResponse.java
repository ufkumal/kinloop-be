package com.kinloop.backend.dto.child;

import com.kinloop.backend.entity.enums.AgeBand;
import com.kinloop.backend.entity.enums.Gender;

import java.time.LocalDate;

public record CreateChildResponse(
        Long childId,
        String fullName,
        Gender gender,
        String displayName,
        LocalDate birthDate,
        int ageMonths,
        AgeBand ageBand,
        SessionSummaryResponse session
) {
}
