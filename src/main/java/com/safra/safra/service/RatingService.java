package com.safra.safra.service;
import java.util.List;
import com.safra.safra.entity.Trip;
import com.safra.safra.entity.TripRating;
import com.safra.safra.entity.User;
import com.safra.safra.repository.TripRatingRepository;
import com.safra.safra.repository.TripRepository;
import com.safra.safra.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final TripRatingRepository ratingRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    @Transactional
    public TripRating rateTrip(Long tripId, Long passengerId, Integer rating, String comment) {
        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        User passenger = userRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found"));

        // Check if trip is completed
        if (!"COMPLETED".equals(trip.getStatus())) {
            throw new RuntimeException("Can only rate completed trips");
        }

        // Check if passenger was part of the trip
        boolean wasPassenger = trip.getPassengers().stream()
                .anyMatch(p -> p.getId().equals(passengerId));

        if (!wasPassenger) {
            throw new RuntimeException("Only passengers who took the trip can rate it");
        }

        // Check if already rated
        if (ratingRepository.existsByTripIdAndPassengerId(tripId, passengerId)) {
            throw new RuntimeException("You have already rated this trip");
        }

        // Create rating
        TripRating tripRating = TripRating.builder()
                .trip(trip)
                .passenger(passenger)
                .rating(rating)
                .comment(comment)
                .build();

        tripRating = ratingRepository.save(tripRating);

        // Update trip average rating
        updateTripRating(trip);

        // Update driver average rating
        updateDriverRating(trip.getDriver());

        return tripRating;
    }

    private void updateTripRating(Trip trip) {
        List<TripRating> ratings = ratingRepository.findByTripId(trip.getId());

        if (!ratings.isEmpty()) {
            double average = ratings.stream()
                    .mapToInt(TripRating::getRating)
                    .average()
                    .orElse(0.0);

            trip.setAverageRating(Math.round(average * 100.0) / 100.0); // Round to 2 decimals
            trip.setTotalRatings(ratings.size());
            tripRepository.save(trip);
        }
    }

    private void updateDriverRating(User driver) {
        // Get all completed trips by this driver
        List<Trip> driverTrips = tripRepository.findByDriverIdAndStatus(
                driver.getId(), Trip.Status.COMPLETED);

        // Get all ratings for these trips
        double totalRating = 0;
        int totalCount = 0;

        for (Trip trip : driverTrips) {
            if (trip.getAverageRating() != null && trip.getTotalRatings() != null) {
                totalRating += trip.getAverageRating() * trip.getTotalRatings();
                totalCount += trip.getTotalRatings();
            }
        }

        if (totalCount > 0) {
            double average = totalRating / totalCount;
            driver.setAverageRating(Math.round(average * 100.0) / 100.0);
            driver.setTotalRatings(totalCount);
            userRepository.save(driver);
        }
    }

    public List<TripRating> getTripRatings(Long tripId) {
        return ratingRepository.findByTripId(tripId);
    }

    public List<TripRating> getDriverRatings(Long driverId) {
        return ratingRepository.findByTrip_Driver_Id(driverId);
    }

    public boolean hasUserRatedTrip(Long tripId, Long passengerId) {
        return ratingRepository.existsByTripIdAndPassengerId(tripId, passengerId);
    }
}
