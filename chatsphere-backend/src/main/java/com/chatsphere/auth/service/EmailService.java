package com.chatsphere.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final boolean mailEnabled;
    private final String frontendUrl;

    public EmailService(
            @Value("${app.mail.enabled:false}") boolean mailEnabled,
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String frontendUrl
    ) {
        this.mailEnabled = mailEnabled;
        this.frontendUrl = frontendUrl.contains(",") ? frontendUrl.split(",")[0].trim() : frontendUrl.trim();
    }

    public void sendVerificationEmail(String recipientEmail, String rawToken) {
        String verificationLink = frontendUrl + "/verify-email?token=" + rawToken;
        if (mailEnabled) {
            // Future SMTP integration hook
            log.info("Sending verification email to: {}", recipientEmail);
        } else {
            log.info("==========================================================");
            log.info("DEV EMAIL (VERIFICATION)");
            log.info("To: {}", recipientEmail);
            log.info("Verification Link: {}", verificationLink);
            log.info("==========================================================");
        }
    }

    public void sendPasswordResetEmail(String recipientEmail, String rawToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
        if (mailEnabled) {
            // Future SMTP integration hook
            log.info("Sending password reset email to: {}", recipientEmail);
        } else {
            log.info("==========================================================");
            log.info("DEV EMAIL (PASSWORD RESET)");
            log.info("To: {}", recipientEmail);
            log.info("Password Reset Link: {}", resetLink);
            log.info("==========================================================");
        }
    }
}
