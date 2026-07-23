package com.kinloop.backend.dto.child;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateChildRequest(
        @Size(max = 255) String fullName,
        @NotNull @PastOrPresent LocalDate birthDate
) {
}
