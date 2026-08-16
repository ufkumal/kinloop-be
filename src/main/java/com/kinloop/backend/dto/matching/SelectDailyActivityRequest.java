package com.kinloop.backend.dto.matching;

import jakarta.validation.constraints.NotNull;

public record SelectDailyActivityRequest(@NotNull Long activityId) {
}
