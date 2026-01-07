package com.safra.safra.controller;

import com.safra.safra.dto.PasswordChangeDTO;
import com.safra.safra.dto.ProfileUpdateDTO;
import com.safra.safra.dto.RideRequestDTO;
import com.safra.safra.entity.RideRequest;
import com.safra.safra.entity.Trip;
import com.safra.safra.entity.User;
import com.safra.safra.repository.TripRepository;
import com.safra.safra.repository.UserRepository;
import com.safra.safra.service.RideRequestService;
import com.safra.safra.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final RideRequestService rideRequestService;
    private final UserService userService;

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#id)")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {

        User user = userRepository.findById(id).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        }
        return ResponseEntity.ok(user);
    }
    @GetMapping("/trips/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#id)")
    public ResponseEntity<?> getUserTrips(@PathVariable Long id) {

        List<Trip> trips = tripRepository.findByDriverId(id);

        if (trips.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No trips found for driver ID: " + id);
        }

        return ResponseEntity.ok(trips);
    }

    @PostMapping("/request")
    public ResponseEntity<?> requestRide(@RequestBody RideRequestDTO dto) {

        Trip trip = tripRepository.findById(dto.getTripId())
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        User passenger = userRepository.findById(dto.getPassengerId())
                .orElseThrow(() -> new RuntimeException("Passenger not found"));

        RideRequest request = dto.toEntity(trip, passenger);

        request = rideRequestService.createRequest(request);

        return ResponseEntity.ok(request);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable Long id) {
        return ResponseEntity.ok(rideRequestService.acceptRideRequest(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id) {
        return ResponseEntity.ok(rideRequestService.rejectRideRequest(id));
    }

    @GetMapping("/driver/{driverId}/requests")
    public ResponseEntity<?> getDriverRideRequests(@PathVariable Long driverId) {
        return ResponseEntity.ok(rideRequestService.getRequestsForDriver(driverId));
    }
    @GetMapping("passenger/{passengerId}/requests")
    public ResponseEntity<?> getPassengerRideRequests(@PathVariable Long passengerId) {
        return ResponseEntity.ok(rideRequestService.getRequestsForPassenger(passengerId));
    }
    @PutMapping("/{userId}/fcm-token")
    public ResponseEntity<?> updateFcmToken(
            @PathVariable Long userId,
            @RequestBody Map<String, String> tokenData) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setFcmToken(tokenData.get("fcmToken"));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== PROFILE MANAGEMENT ====================

    /**
     * Update user profile information (name, phone, gender, birthDate, studentEmail)
     */
    @PutMapping("/{userId}/profile")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#userId)")
    public ResponseEntity<?> updateProfile(
            @PathVariable Long userId,
            @RequestBody ProfileUpdateDTO profileDTO) {
        try {
            User updatedUser = userService.updateProfile(userId, profileDTO);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile updated successfully",
                    "user", updatedUser
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Upload/Update profile picture
     * Accepts multipart/form-data with a file field named "file"
     */
    @PostMapping(value = "/{userId}/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#userId)")
    public ResponseEntity<?> uploadProfilePicture(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = userService.updateProfilePicture(userId, file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Failed to upload profile picture: " + e.getMessage()
            ));
        }
    }

    /**
     * Remove profile picture
     */
    @DeleteMapping("/{userId}/profile-picture")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#userId)")
    public ResponseEntity<?> removeProfilePicture(@PathVariable Long userId) {
        try {
            Map<String, Object> result = userService.removeProfilePicture(userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Change user password
     * Requires old password verification
     */
    @PostMapping("/{userId}/change-password")
    @PreAuthorize("@userSecurity.isSelf(#userId)")
    public ResponseEntity<?> changePassword(
            @PathVariable Long userId,
            @RequestBody PasswordChangeDTO passwordDTO) {
        try {
            Map<String, Object> result = userService.changePassword(userId, passwordDTO);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
