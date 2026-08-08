package com.kinloop.backend.dto.consent;

import jakarta.validation.constraints.NotNull;

public record UpdateConsentRequest(@NotNull Boolean granted) {}
