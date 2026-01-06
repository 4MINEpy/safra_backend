package com.safra.safra.repository;

import com.safra.safra.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    // Add this method for NotificationService
    List<Trip> findByStartTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    List<Trip> findByDriverId(Long driverId);

    @Query(value = """
WITH trip_scores AS (
    SELECT 
        t.*,
        ST_Distance(
            t.start_location::geography,
            ST_SetSRID(ST_MakePoint(:departureLng, :departureLat), 4326)::geography
        ) as start_dist,
        ST_Distance(
            t.end_location::geography,
            ST_SetSRID(ST_MakePoint(:destinationLng, :destinationLat), 4326)::geography
        ) as end_dist,
        degrees(ST_Azimuth(
            ST_SetSRID(ST_MakePoint(:departureLng, :departureLat), 4326),
            ST_SetSRID(ST_MakePoint(:destinationLng, :destinationLat), 4326)
        )) as desired_azimuth,
        degrees(ST_Azimuth(t.start_location::geometry, t.end_location::geometry)) as trip_azimuth
    FROM trips t
    WHERE t.status = 'OPEN'
)
SELECT 
    t.*,
    ROUND(start_dist::numeric, 2) as start_distance_m,
    ROUND(end_dist::numeric, 2) as end_distance_m,
    ROUND(ABS(desired_azimuth - trip_azimuth)::numeric, 1) as direction_diff_degrees
FROM trip_scores t
WHERE start_dist <= 6500 AND end_dist <= 6500
ORDER BY 
    CASE 
        WHEN start_dist <= 2000 AND end_dist <= 2000 THEN 1
        WHEN start_dist <= 2000 OR end_dist <= 2000 THEN 2
        ELSE 3
    END,
    (start_dist + end_dist)
""", nativeQuery = true)
    List<Trip> findTripsWithinDistance(
            @Param("departureLat") double departureLat,
            @Param("departureLng") double departureLng,
            @Param("destinationLat") double destinationLat,
            @Param("destinationLng") double destinationLng
    );

    @Query("SELECT t FROM Trip t WHERE t.status = 'ACTIVE' AND t.currentDriverLat IS NOT NULL")
    List<Trip> findActiveTripsWithLocation();

    // Find trips by status
    List<Trip> findByStatus(String status);

    // Find trips by driver and status
    List<Trip> findByDriverIdAndStatus(Long driverId, String status);
}