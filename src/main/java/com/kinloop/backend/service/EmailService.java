package com.kinloop.backend.service;

public interface EmailService {

    String  sendVerificationEmail(String toEmail, String token);
}
