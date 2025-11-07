package com.safra.safra.controller;

import com.safra.safra.entity.Role;
import com.safra.safra.entity.User;
import com.safra.safra.repository.UserRepository;
import com.safra.safra.security.JwtUtil;
import com.safra.safra.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
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

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setGender(user.getGender());
        user.setBirthDate(user.getBirthDate());
        user.setPhoneNumber(user.getPhoneNumber());
        user.setEmail(user.getEmail());
        user.setIsBanned(false);
        user.setEmailVerified(false);
        if(user.getProfilePicture() != null) {
            user.setProfilePicture(user.getProfilePicture());
        }
        user.setRole(Role.CLIENT);
        user.setJoinDate(LocalDateTime.now());
        userRepository.save(user);


        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            // Load user entity from DB
            User user = userRepository.findByEmail(credentials.get("email"))
                    .orElseThrow(() -> new Exception("User not found"));

            if (Boolean.TRUE.equals(user.getIsBanned())) {
                throw new Exception("This account is banned");
            }

            if (!Boolean.TRUE.equals(user.isEmailVerified())) {
                throw new Exception("Email not verified");
            }

            if (!passwordEncoder.matches(credentials.get("password"), user.getPassword())) {
                throw new Exception("Incorrect password");
            }

            String token = jwtUtil.generateToken(userDetailsService.loadUserByUsername(user.getEmail()));
            return ResponseEntity.ok(Map.of("token", token));

        } catch (Exception e) {
            System.out.println("Login attempt: " + credentials.get("email") + " failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

}

