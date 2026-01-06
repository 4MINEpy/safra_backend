package com.safra.safra.service;

import com.safra.safra.entity.EmailVerificationToken;
import com.safra.safra.entity.User;
import com.safra.safra.repository.EmailVerificationTokenRepository;
import com.safra.safra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8087}")
    private String baseUrl;

    @Value("${spring.mail.enabled:true}")
    private boolean emailEnabled;

    // List of valid student email domains (can be expanded)
    private static final String[] VALID_STUDENT_DOMAINS = {
            ".edu",
            ".r-iset.tn",
            ".edu.tn",
            ".ac.uk",
            ".edu.au",
            ".edu.fr",
            "etu.",       // For emails like name@etu.university.tn
            "student."    // For emails like name@student.university.edu
    };

    /**
     * Check if email is a valid student email
     */
    public boolean isValidStudentEmail(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }

        String domain = email.substring(email.indexOf("@")).toLowerCase();

        for (String validDomain : VALID_STUDENT_DOMAINS) {
            if (domain.contains(validDomain)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Request student email verification
     */
    @Transactional
    public void requestStudentVerification(Long userId, String studentEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if student email is valid format
        if (!isValidStudentEmail(studentEmail)) {
            throw new RuntimeException("Invalid student email. Please use your college/university email address.");
        }

        // Check if this student email is already used by another user
        Optional<User> existingUser = userRepository.findAll().stream()
                .filter(u -> studentEmail.equalsIgnoreCase(u.getStudentEmail()) && !u.getId().equals(userId))
                .findFirst();

        if (existingUser.isPresent()) {
            throw new RuntimeException("This student email is already registered to another account.");
        }

        // Set student email (not verified yet)
        user.setStudentEmail(studentEmail);
        user.setStudentVerified(false);
        userRepository.save(user);

        // Remove any existing verification tokens for this user
        tokenRepository.findByUserEmail(user.getEmail()).ifPresent(tokenRepository::delete);

        // Create new verification token
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = new EmailVerificationToken(token, user);
        tokenRepository.save(verificationToken);

        // Send verification email to student email
        sendStudentVerificationEmail(studentEmail, token, user.getName());

        log.info("📧 Student verification email sent to: {} for user: {}", studentEmail, user.getEmail());
    }

    /**
     * Verify student email with token
     */
    @Transactional
    public boolean verifyStudentEmail(String token) {
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            log.warn("❌ Invalid student verification token");
            return false;
        }

        EmailVerificationToken verificationToken = tokenOpt.get();

        if (verificationToken.isExpired()) {
            tokenRepository.delete(verificationToken);
            log.warn("❌ Student verification token expired");
            return false;
        }

        User user = verificationToken.getUser();
        user.setStudentVerified(true);
        userRepository.save(user);

        tokenRepository.delete(verificationToken);

        log.info("✅ Student email verified for user: {}", user.getEmail());
        return true;
    }

    /**
     * Send student verification email
     */
    private void sendStudentVerificationEmail(String toEmail, String token, String userName) {
        if (!emailEnabled) {
            logDevelopmentMode(toEmail, token);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setFrom("aminmedfai90@gmail.com");
            message.setSubject("Verify Your Student Email - SAFRA");
            message.setText(
                    "Hello " + userName + ",\n\n" +
                            "You requested to verify your student email to get access to the student subscription plan.\n\n" +
                            "Please click the link below to verify your student email:\n" +
                            baseUrl + "/auth/verify-student-email?token=" + token + "\n\n" +
                            "This link expires in 24 hours.\n\n" +
                            "If you didn't request this, please ignore this email.\n\n" +
                            "Thank you,\n" +
                            "SAFRA Team"
            );

            mailSender.send(message);
            log.info("✅ Student verification email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send student verification email: {}", e.getMessage());
            logDevelopmentMode(toEmail, token);
        }
    }

    private void logDevelopmentMode(String toEmail, String token) {
        log.info("🔐 DEVELOPMENT MODE - Student verification email would be sent to: {}", toEmail);
        log.info("🔑 Token: {}", token);
        log.info("🔗 Link: {}/auth/verify-student-email?token={}", baseUrl, token);

        System.out.println("\n" + "=".repeat(70));
        System.out.println("📧 STUDENT VERIFICATION (DEVELOPMENT MODE)");
        System.out.println("📧 To: " + toEmail);
        System.out.println("🔑 Token: " + token);
        System.out.println("🔗 Link: " + baseUrl + "/auth/verify-student-email?token=" + token);
        System.out.println("=".repeat(70) + "\n");
    }

    /**
     * Check if user has verified student status
     */
    public boolean isStudentVerified(Long userId) {
        return userRepository.findById(userId)
                .map(User::getStudentVerified)
                .orElse(false);
    }
}
