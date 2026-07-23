package com.kinloop.backend.service.impl;

import com.kinloop.backend.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsoleEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailService.class);
    private static final String VERIFY_URL = "https://kinloop-be.onrender.com/verify?token=%s";

    @Override
    public void sendVerificationEmail(String toEmail, String token) {
        log.info("Verification link for {}: {}", toEmail, VERIFY_URL.formatted(token));
    }
}
