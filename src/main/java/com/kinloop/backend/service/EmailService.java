package com.kinloop.backend.service;

public interface EmailService {

    void sendVerificationEmail(String toEmail, String token);
}
