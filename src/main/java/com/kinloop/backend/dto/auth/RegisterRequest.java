package com.kinloop.backend.dto.auth;

import com.kinloop.backend.entity.enums.UserRole;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8)
        String password,

        @NotNull
        UserRole role
) {

    @AssertTrue(message = "role must be PARENT or WORKSHOP_OWNER")
    public boolean isPublicRegistrationRole() {
        return role == UserRole.PARENT || role == UserRole.WORKSHOP_OWNER;
    }
}
