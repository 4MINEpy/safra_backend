package com.safra.safra.controller;

import com.safra.safra.dto.RideRequestDTO;
import com.safra.safra.entity.RideRequest;
import com.safra.safra.entity.Trip;
import com.safra.safra.entity.User;
import com.safra.safra.repository.TripRepository;
import com.safra.safra.repository.UserRepository;
import com.safra.safra.service.RideRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final RideRequestService rideRequestService;

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
}
