package com.safra.safra.controller;

import com.safra.safra.dto.StudentEmailVerificationDTO;
import com.safra.safra.entity.Role;
import com.safra.safra.entity.User;
import com.safra.safra.entity.EmailVerificationToken;
import com.safra.safra.repository.UserRepository;
import com.safra.safra.repository.EmailVerificationTokenRepository;
import com.safra.safra.service.EmailService;
import com.safra.safra.security.JwtUtil;
import com.safra.safra.service.CustomUserDetailsService;
import com.safra.safra.service.StudentVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final StudentVerificationService studentVerificationService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        // Set user properties - Email is automatically verified after registration
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setIsBanned(false);
        user.setEmailVerified(false);
        user.setRole(Role.CLIENT);
        user.setJoinDate(LocalDateTime.now());

        // Save user
        User savedUser = userRepository.save(user);

        // Create verification token and send email
        try {
            String token = java.util.UUID.randomUUID().toString();
            EmailVerificationToken verificationToken = new EmailVerificationToken(token, savedUser);
            tokenRepository.save(verificationToken);
            emailService.sendVerificationEmail(savedUser.getEmail(), token);
        } catch (Exception e) {
            // Log and continue — registration succeeded but email sending failed
            System.out.println("⚠️ Failed to create/send verification token: " + e.getMessage());
        }

        System.out.println("✅ USER REGISTERED: " + savedUser.getEmail());

        return ResponseEntity.ok("User registered successfully. Please check your email to verify the account.");
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            var tokenOpt = tokenRepository.findByToken(token);
            if (tokenOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired verification token");
            }
            EmailVerificationToken verificationToken = tokenOpt.get();
            if (verificationToken.isExpired()) {
                tokenRepository.delete(verificationToken);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Verification token has expired");
            }
            User user = verificationToken.getUser();
            user.setEmailVerified(true);
            userRepository.save(user);
            tokenRepository.delete(verificationToken);
            return ResponseEntity.ok("Email verified successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred verifying email");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            User user = userRepository.findByEmail(credentials.get("email"))
                    .orElseThrow(() -> new Exception("User not found"));

            if (!passwordEncoder.matches(credentials.get("password"), user.getPassword())) {
                throw new Exception("Incorrect password");
            }
            if(!user.isEmailVerified()){
                throw new Exception("Email not verified, Check you E-mail for a verification link !");
            }

            // Generate JWT
            String token = jwtUtil.generateToken(userDetailsService.loadUserByUsername(user.getEmail()));

            // Return token + user info
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", Map.of(
                    "id", user.getId(),
                    "name", user.getName(),
                    "email", user.getEmail(),
                    "role", user.getRole()
            ));

            System.out.println("✅ LOGIN SUCCESSFUL: " + user.getEmail());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("❌ LOGIN FAILED: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping("/adminlogin")
    public ResponseEntity<?> loginAdmin(@RequestBody Map<String, String> credentials) {
        try {
            User user = userRepository.findByEmail(credentials.get("email"))
                    .orElseThrow(() -> new Exception("User not found"));
            if (!passwordEncoder.matches(credentials.get("password"), user.getPassword())) {
                throw new Exception("Incorrect password");
            }
            if (user.getRole() != Role.ADMIN) {
                throw new Exception("Access denied: Admins only");
            }

            // Generate JWT
            String token = jwtUtil.generateToken(userDetailsService.loadUserByUsername(user.getEmail()));

            // Return token + user info
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", Map.of(
                    "id", user.getId(),
                    "name", user.getName(),
                    "email", user.getEmail(),
                    "role", user.getRole()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============ STUDENT EMAIL VERIFICATION ============

    /**
     * Request student email verification
     * Body: { "userId": 1, "studentEmail": "student@university.edu" }
     */
    @PostMapping("/request-student-verification")
    public ResponseEntity<?> requestStudentVerification(@RequestBody StudentEmailVerificationDTO dto) {
        try {
            studentVerificationService.requestStudentVerification(dto.getUserId(), dto.getStudentEmail());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Verification email sent to " + dto.getStudentEmail()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Verify student email with token
     */
    @GetMapping("/verify-student-email")
    public ResponseEntity<?> verifyStudentEmail(@RequestParam String token) {
        boolean verified = studentVerificationService.verifyStudentEmail(token);
        if (verified) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Student email verified successfully! You can now purchase the student subscription plan."
            ));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", "Invalid or expired verification token"
        ));
    }

    /**
     * Check if a user's student email is verified
     */
    @GetMapping("/student-status/{userId}")
    public ResponseEntity<?> getStudentStatus(@PathVariable Long userId) {
        boolean isVerified = studentVerificationService.isStudentVerified(userId);
        User user = userRepository.findById(userId).orElse(null);
        
        return ResponseEntity.ok(Map.of(
                "studentVerified", isVerified,
                "studentEmail", user != null && user.getStudentEmail() != null ? user.getStudentEmail() : ""
        ));
    }
}