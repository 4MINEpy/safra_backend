package com.safra.safra.repository;


import com.safra.safra.entity.TripRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TripRating entity
 */
@Repository
public interface TripRatingRepository extends JpaRepository<TripRating, Long> {

    /**
     * Find all ratings for a specific trip
     * @param tripId The trip ID
     * @return List of ratings for the trip
     */
    List<TripRating> findByTripId(Long tripId);

    /**
     * Find all ratings given by a specific passenger
     * @param passengerId The passenger ID
     * @return List of ratings given by the passenger
     */
    List<TripRating> findByPassengerId(Long passengerId);

    /**
     * Find all ratings for trips driven by a specific driver
     * @param driverId The driver ID
     * @return List of ratings for the driver's trips
     */
    List<TripRating> findByTrip_Driver_Id(Long driverId);

    /**
     * Check if a passenger has already rated a specific trip
     * @param tripId The trip ID
     * @param passengerId The passenger ID
     * @return true if rating exists, false otherwise
     */
    boolean existsByTripIdAndPassengerId(Long tripId, Long passengerId);

    /**
     * Find a specific rating by trip and passenger
     * @param tripId The trip ID
     * @param passengerId The passenger ID
     * @return Optional containing the rating if it exists
     */
    Optional<TripRating> findByTripIdAndPassengerId(Long tripId, Long passengerId);

    /**
     * Get average rating for a specific trip
     * @param tripId The trip ID
     * @return Average rating as Double
     */
    @Query("SELECT AVG(r.rating) FROM TripRating r WHERE r.trip.id = :tripId")
    Double getAverageRatingForTrip(@Param("tripId") Long tripId);

    /**
     * Get average rating for all trips by a specific driver
     * @param driverId The driver ID
     * @return Average rating as Double
     */
    @Query("SELECT AVG(r.rating) FROM TripRating r WHERE r.trip.driver.id = :driverId")
    Double getAverageRatingForDriver(@Param("driverId") Long driverId);

    /**
     * Count total ratings for a specific trip
     * @param tripId The trip ID
     * @return Number of ratings
     */
    Long countByTripId(Long tripId);

    /**
     * Count total ratings for all trips by a specific driver
     * @param driverId The driver ID
     * @return Number of ratings
     */
    @Query("SELECT COUNT(r) FROM TripRating r WHERE r.trip.driver.id = :driverId")
    Long countByDriverId(@Param("driverId") Long driverId);

    /**
     * Find recent ratings for a driver (last N ratings)
     * @param driverId The driver ID
     * @param limit Maximum number of ratings to return
     * @return List of recent ratings
     */
    @Query(value = "SELECT * FROM trip_ratings r " +
            "JOIN trips t ON r.trip_id = t.id " +
            "WHERE t.driver_id = :driverId " +
            "ORDER BY r.created_at DESC LIMIT :limit",
            nativeQuery = true)
    List<TripRating> findRecentRatingsForDriver(
            @Param("driverId") Long driverId,
            @Param("limit") int limit
    );
}