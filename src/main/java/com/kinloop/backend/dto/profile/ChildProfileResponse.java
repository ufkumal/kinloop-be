package com.kinloop.backend.dto.profile;

import com.kinloop.backend.entity.enums.Gender;
import com.kinloop.backend.entity.enums.PreferenceMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ChildProfileResponse(
        Long childId,
        String fullName,
        String displayName,
        LocalDate birthDate,
        int ageMonths,
        Gender gender,
        PreferenceMode preferenceMode,
        OffsetDateTime onboardingCompletedAt,
        List<ProfileAnswerResponse> onboardingAnswers
) {
}
