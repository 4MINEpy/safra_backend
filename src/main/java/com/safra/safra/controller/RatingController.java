package com.safra.safra.controller;

import com.safra.safra.dto.RatingRequestDTO;
import com.safra.safra.entity.TripRating;
import com.safra.safra.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<?> rateTrip(@RequestBody RatingRequestDTO dto) {
        try {
            TripRating rating = ratingService.rateTrip(
                    dto.getTripId(),
                    dto.getPassengerId(),
                    dto.getRating(),
                    dto.getComment()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Rating submitted successfully");
            response.put("rating", rating);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<?> getTripRatings(@PathVariable Long tripId) {
        List<TripRating> ratings = ratingService.getTripRatings(tripId);
        return ResponseEntity.ok(ratings);
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<?> getDriverRatings(@PathVariable Long driverId) {
        List<TripRating> ratings = ratingService.getDriverRatings(driverId);
        return ResponseEntity.ok(ratings);
    }

    @GetMapping("/check")
    public ResponseEntity<?> hasUserRatedTrip(
            @RequestParam Long tripId,
            @RequestParam Long passengerId) {
        boolean hasRated = ratingService.hasUserRatedTrip(tripId, passengerId);
        return ResponseEntity.ok(Map.of("hasRated", hasRated));
    }
}

