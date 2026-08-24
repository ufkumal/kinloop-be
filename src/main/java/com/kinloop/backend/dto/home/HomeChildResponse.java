package com.kinloop.backend.dto.home;

import com.kinloop.backend.entity.enums.AgeBand;
import com.kinloop.backend.entity.enums.Gender;
import java.time.LocalDate;

public record HomeChildResponse(
        Long childId,
        String fullName,
        String displayName,
        LocalDate birthDate,
        int ageMonths,
        AgeBand ageBand,
        Gender gender
) {
}
