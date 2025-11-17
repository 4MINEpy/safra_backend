package com.safra.safra.controller;

import com.safra.safra.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    // Step 1: Request a password reset. Body: { "email": "user@example.com" }
    @PostMapping("/request-password-reset")
    public ResponseEntity<?> requestReset(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing email"));
        }
        boolean exists = passwordResetService.requestPasswordReset(email.trim());
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // Step 3: Verify code and reset password. Body: { "email": "..", "code": "..", "newPassword": ".." }
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        String newPassword = body.get("newPassword");

        if (email == null || code == null || newPassword == null || email.isBlank() || code.isBlank() || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
        }

        boolean ok = passwordResetService.verifyCodeAndReset(email.trim(), code.trim(), newPassword);
        if (ok) return ResponseEntity.ok(Map.of("success", true, "message", "Password reset successful"));
        return ResponseEntity.status(400).body(Map.of("success", false, "message", "Invalid or expired code"));
    }
}
