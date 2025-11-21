package com.safra.safra.controller;

import com.safra.safra.entity.Trip;
import com.safra.safra.entity.User;
import com.safra.safra.repository.TripRepository;
import com.safra.safra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;

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

}
