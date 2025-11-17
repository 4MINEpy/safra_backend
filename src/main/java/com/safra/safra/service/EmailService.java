package com.safra.safra.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8087}")
    private String baseUrl;

    @Value("${spring.mail.enabled:true}")
    private boolean emailEnabled;

    public void sendVerificationEmail(String toEmail, String token) {
        log.info("📧 Attempting to send verification email to: {}", toEmail);
        log.info("📧 Email enabled: {}", emailEnabled);

        if (!emailEnabled) {
            logDevelopmentMode(toEmail, token);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setFrom("youssoufa003@gmail.com");
            message.setSubject("Verify Your Email - SAFRA");
            message.setText(
                    "Welcome to SAFRA!\n\n" +
                            "Please click the link below to verify your email address:\n" +
                            baseUrl + "/auth/verify-email?token=" + token + "\n\n" +
                            "Or enter this verification code in the app: " + token + "\n\n" +
                            "If you didn't create an account, please ignore this email.\n\n" +
                            "Thank you,\n" +
                            "SAFRA Team"
            );

            mailSender.send(message);
            log.info("✅ Verification email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send verification email to {}: {}", toEmail, e.getMessage(), e);
            logDevelopmentMode(toEmail, token);
        }
    }

    private void logDevelopmentMode(String toEmail, String token) {
        log.info("🔐 DEVELOPMENT MODE - Email would be sent to: {}", toEmail);
        log.info("🔑 Token: {}", token);
        log.info("🔗 Link: {}/auth/verify-email?token={}", baseUrl, token);

        System.out.println("\n" + "=".repeat(70));
        System.out.println("📧 EMAIL VERIFICATION (DEVELOPMENT MODE)");
        System.out.println("📧 To: " + toEmail);
        System.out.println("🔑 Token: " + token);
        System.out.println("🔗 Verification Link: " + baseUrl + "/auth/verify-email?token=" + token);
        System.out.println("=".repeat(70) + "\n");
    }
}