package com.kinloop.backend.dto.auth;

import com.kinloop.backend.entity.enums.UserRole;

public record AuthResponse(
        String token,
        UserRole role,
        Long userId,
        boolean emailVerified
) {
}
