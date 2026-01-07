package com.safra.safra.service;

import com.safra.safra.dto.PasswordChangeDTO;
import com.safra.safra.dto.ProfileUpdateDTO;
import com.safra.safra.entity.User;
import com.safra.safra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    /**
     * Update user profile information
     */
    @Transactional
    public User updateProfile(Long userId, ProfileUpdateDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update only non-null fields
        if (dto.getName() != null && !dto.getName().isBlank()) {
            user.setName(dto.getName());
        }
        
        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(dto.getPhoneNumber());
        }
        
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        
        if (dto.getBirthDate() != null) {
            user.setBirthDate(dto.getBirthDate());
        }
        
        if (dto.getStudentEmail() != null) {
            // If student email is being updated, reset verification
            if (!dto.getStudentEmail().equals(user.getStudentEmail())) {
                user.setStudentEmail(dto.getStudentEmail());
                user.setStudentVerified(false);
            }
        }

        User updatedUser = userRepository.save(user);
        log.info("✅ Profile updated for user: {}", userId);
        
        return updatedUser;
    }

    /**
     * Update user profile picture
     */
    @Transactional
    public Map<String, Object> updateProfilePicture(Long userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete old profile picture if exists
        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            fileStorageService.deleteFile(user.getProfilePicture());
        }

        // Store new profile picture
        String imageUrl = fileStorageService.storeProfilePicture(file, userId);
        user.setProfilePicture(imageUrl);
        userRepository.save(user);

        log.info("✅ Profile picture updated for user: {}", userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("profilePictureUrl", imageUrl);
        response.put("message", "Profile picture updated successfully");
        
        return response;
    }

    /**
     * Remove user profile picture
     */
    @Transactional
    public Map<String, Object> removeProfilePicture(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            fileStorageService.deleteFile(user.getProfilePicture());
            user.setProfilePicture(null);
            userRepository.save(user);
            log.info("✅ Profile picture removed for user: {}", userId);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Profile picture removed successfully");
        
        return response;
    }

    /**
     * Change user password
     */
    @Transactional
    public Map<String, Object> changePassword(Long userId, PasswordChangeDTO dto) {
        Map<String, Object> response = new HashMap<>();

        // Validation
        if (dto.getOldPassword() == null || dto.getOldPassword().isBlank()) {
            response.put("success", false);
            response.put("error", "Old password is required");
            return response;
        }

        if (dto.getNewPassword() == null || dto.getNewPassword().isBlank()) {
            response.put("success", false);
            response.put("error", "New password is required");
            return response;
        }

        if (dto.getNewPassword().length() < 6) {
            response.put("success", false);
            response.put("error", "New password must be at least 6 characters long");
            return response;
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            response.put("success", false);
            response.put("error", "New password and confirm password do not match");
            return response;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify old password
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            response.put("success", false);
            response.put("error", "Current password is incorrect");
            return response;
        }

        // Check if new password is same as old
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            response.put("success", false);
            response.put("error", "New password must be different from current password");
            return response;
        }

        // Update password
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        log.info("✅ Password changed for user: {}", userId);

        response.put("success", true);
        response.put("message", "Password changed successfully");
        
        return response;
    }

    /**
     * Get user profile by ID
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
