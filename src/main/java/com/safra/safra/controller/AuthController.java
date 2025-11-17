package com.safra.safra.controller;

import com.safra.safra.entity.Role;
import com.safra.safra.entity.User;
import com.safra.safra.repository.UserRepository;
import com.safra.safra.security.JwtUtil;
import com.safra.safra.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        // Set user properties - Email is automatically verified after registration
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setIsBanned(false);
        user.setEmailVerified(true); // Set to true - no email verification required
        user.setRole(Role.CLIENT);
        user.setJoinDate(LocalDateTime.now());

        // Save user
        User savedUser = userRepository.save(user);

        System.out.println("✅ USER REGISTERED: " + savedUser.getEmail());
        System.out.println("✅ USER CAN LOGIN IMMEDIATELY: " + savedUser.getEmail());

        return ResponseEntity.ok("User registered successfully. You can now login.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            User user = userRepository.findByEmail(credentials.get("email"))
                    .orElseThrow(() -> new Exception("User not found"));

            if (!passwordEncoder.matches(credentials.get("password"), user.getPassword())) {
                throw new Exception("Incorrect password");
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
}