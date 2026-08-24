package com.kinloop.backend.dto.child;

import com.kinloop.backend.entity.enums.AgeBand;
import com.kinloop.backend.entity.enums.Gender;
import java.time.LocalDate;

public record UpdateChildResponse(
        Long childId,
        String fullName,
        String displayName,
        LocalDate birthDate,
        int ageMonths,
        AgeBand ageBand,
        Gender gender,
        boolean questionnaireRestarted
) {
}
