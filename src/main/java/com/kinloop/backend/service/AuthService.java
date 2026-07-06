package com.kinloop.backend.service;

import com.kinloop.backend.dto.auth.AuthResponse;
import com.kinloop.backend.dto.auth.LoginRequest;
import com.kinloop.backend.dto.auth.RegisterRequest;
import com.kinloop.backend.dto.auth.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest req);

    void verifyEmail(String token);

    AuthResponse login(LoginRequest req, String userAgent);
}
