package com.safra.safra.controller;

import com.safra.safra.entity.Role;
import com.safra.safra.entity.User;
import com.safra.safra.repository.UserRepository;
import com.safra.safra.security.JwtUtil;
import com.safra.safra.service.CustomUserDetailsService;
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

    @PostMapping("/adminlogin")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
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

