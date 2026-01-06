package com.safra.safra.service;

import com.safra.safra.dto.DriverLocationUpdateDTO;
import com.safra.safra.dto.TripRequestDTO;
import com.safra.safra.entity.RequestStatus;
import com.safra.safra.entity.RideRequest;
import com.safra.safra.entity.Trip;
import com.safra.safra.entity.User;
import com.safra.safra.config.OSRMConfig;
import com.safra.safra.repository.RideRequestRepository;
import com.safra.safra.repository.SubscriptionRepository;
import com.safra.safra.repository.TripRepository;
import com.safra.safra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final RideRequestRepository rideRequestRepository;
    private final OSRMConfig osrmConfig;
    private NotificationService notificationService;
    private final SubscriptionService subscriptionService;
    // Add this setter injection method
    @Autowired
    @Lazy
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
        log.info("NotificationService injected into TripService");
    }


    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    public Optional<Trip> getTripById(Long id) {
        return tripRepository.findById(id);
    }

    /**
     * Create a trip - requires valid subscription with remaining trips
     */
    @Transactional
    public Trip createTrip(TripRequestDTO dto) {
        // First, verify the driver has a valid subscription
        if (!subscriptionService.canUserCreateTrip(dto.getDriverId())) {
            throw new RuntimeException("You need an active subscription with remaining trips to create a trip. Please purchase or renew your subscription.");
        }

        Trip trip = new Trip();

        // Set driver
        trip.setDriver(userRepository.findById(dto.getDriverId())
                .orElseThrow(() -> new RuntimeException("Driver not found")));

        // Set passengers
        List<User> passengers = userRepository.findAllById(dto.getPassengerIds());
        trip.setPassengers(passengers);

        // Convert coordinates to Point
        GeometryFactory geometryFactory = new GeometryFactory();
        Point start = geometryFactory.createPoint(new Coordinate(dto.getStartX(), dto.getStartY()));
        Point end = geometryFactory.createPoint(new Coordinate(dto.getEndX(), dto.getEndY()));
        trip.setStartLocation(start);
        trip.setEndLocation(end);

        // Set other fields
        trip.setStartTime(dto.getStartTime());
        trip.setDescription(dto.getDescription());
        trip.setAvailableSeats(dto.getAvailableSeats());
        trip.setPrice(dto.getPrice());
        trip.setStatus(dto.getStatus());

        // Use one trip from the subscription
        subscriptionService.useTrip(dto.getDriverId());

        // Save and return the trip
        return tripRepository.save(trip);
    }

    public void deleteTrip(Long id) {
        tripRepository.deleteById(id);
    }

    public Trip updateTrip(TripRequestDTO dto) {
        Trip trip = tripRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        // If needed, update driver
        if (dto.getDriverId() != null) {
            User driver = userRepository.findById(dto.getDriverId())
                    .orElseThrow(() -> new RuntimeException("Driver not found"));
            trip.setDriver(driver);
        }

        // If needed, update passengers
        if (dto.getPassengerIds() != null) {
            trip.setPassengers(userRepository.findAllById(dto.getPassengerIds()));
        }

        // Update locations
        GeometryFactory geometryFactory = new GeometryFactory();
        Point start = geometryFactory.createPoint(new Coordinate(dto.getStartX(), dto.getStartY()));
        Point end   = geometryFactory.createPoint(new Coordinate(dto.getEndX(), dto.getEndY()));
        trip.setStartLocation(start);
        trip.setEndLocation(end);

        // Update other fields
        trip.setStartTime(dto.getStartTime());
        trip.setDescription(dto.getDescription());
        trip.setAvailableSeats(dto.getAvailableSeats());
        trip.setPrice(dto.getPrice());
        trip.setStatus(dto.getStatus());

        return tripRepository.save(trip);
    }

    public Trip setTripArchiveStatus(Long tripId, Boolean archived) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + tripId));
        trip.setIs_archived(archived);
        return tripRepository.save(trip);
    }

    public List<User> getPassengersForTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        return trip.getPassengers();
    }

    public void removePassengerFromTrip(Long tripId, Long passengerId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        // Find the passenger
        User passenger = trip.getPassengers().stream()
                .filter(p -> p.getId().equals(passengerId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Passenger not found in this trip"));

        // Remove passenger
        trip.getPassengers().remove(passenger);

        // Free the seat
        trip.setAvailableSeats(trip.getAvailableSeats() + 1);

        // Persist changes
        tripRepository.save(trip);
    }

    public Trip cancelTrip(Long tripId, Long driverId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        if (!trip.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("Only the driver can cancel the trip");
        }

        // Update trip status
        trip.setStatus("CANCELED");

        // Cancel all requests linked to this trip
        List<RideRequest> requests = rideRequestRepository.findByTrip_Id(tripId);
        for (RideRequest req : requests) {
            req.setStatus(RequestStatus.CANCELLED);
            req.setUpdatedAt(LocalDateTime.now());
        }

        rideRequestRepository.saveAll(requests);
        User driver = trip.getDriver();

        // Add null check for notificationService
        if (notificationService != null) {
            // Notify all passengers
            for (User passenger : trip.getPassengers()) {
                notificationService.sendTripCancellation(passenger, trip, false);
            }

            // Notify driver
            notificationService.sendTripCancellation(driver, trip, true);
        } else {
            log.warn("NotificationService is null, skipping notifications");
        }

        return tripRepository.save(trip);
    }

    public Trip removePassenger(Long tripId, Long passengerId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        User passenger = userRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found"));

        if (!trip.getPassengers().contains(passenger)) {
            throw new RuntimeException("Passenger not part of this trip");
        }

        trip.getPassengers().remove(passenger);
        trip.setAvailableSeats(trip.getAvailableSeats() + 1);

        return tripRepository.save(trip);
    }

    public List<Trip> findTripsWithinDistance(double startLat, double startLng,
                                              double endLat, double endLng) {
        return tripRepository.findTripsWithinDistance(
                startLat, startLng,
                endLat, endLng );
    }

    // Add this method for NotificationService
    public List<Trip> getTripsStartingBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return tripRepository.findByStartTimeBetween(startTime, endTime);
    }

    public Trip updateTripStatus(Long tripId, String status) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        // Validate status
        List<String> validStatuses = List.of("OPEN", "SCHEDULED", "ACTIVE", "COMPLETED", "CANCELED");
        if (!validStatuses.contains(status.toUpperCase())) {
            throw new RuntimeException("Invalid status. Must be one of: " + validStatuses);
        }

        trip.setStatus(status.toUpperCase());
        return tripRepository.save(trip);
    }

    public Trip updateDriverLocation(Long tripId, Double lat, Double lng,
                                     Double speed, Float bearing, Float accuracy) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        // Check if trip is active
        if (!"ACTIVE".equals(trip.getStatus())) {
            throw new RuntimeException("Cannot update location for non-active trip");
        }

        trip.setCurrentDriverLat(lat);
        trip.setCurrentDriverLng(lng);
        trip.setDriverSpeed(speed);
        trip.setDriverBearing(bearing);
        trip.setLastLocationUpdate(LocalDateTime.now());

        return tripRepository.save(trip);
    }

    public DriverLocationUpdateDTO getDriverLocation(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        if (trip.getCurrentDriverLat() == null || trip.getCurrentDriverLng() == null) {
            throw new RuntimeException("Driver location not available");
        }

        DriverLocationUpdateDTO location = new DriverLocationUpdateDTO();
        location.setLatitude(trip.getCurrentDriverLat());
        location.setLongitude(trip.getCurrentDriverLng());
        location.setSpeed(trip.getDriverSpeed());
        location.setBearing(trip.getDriverBearing());
        location.setTimestamp(trip.getLastLocationUpdate());

        return location;
    }

    public Trip startNavigation(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        // Verify driver is starting their own trip
        // (You might want to add authentication/authorization here)

        trip.setStatus("ACTIVE");
        return tripRepository.save(trip);
    }

    public Trip endNavigation(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        trip.setStatus("COMPLETED");

        // Clear navigation data
        trip.setCurrentDriverLat(null);
        trip.setCurrentDriverLng(null);
        trip.setDriverSpeed(null);
        trip.setDriverBearing(null);
        trip.setLastLocationUpdate(null);

        // After trip completion - add null check
        if (notificationService != null) {
            for (User passenger : trip.getPassengers()) {
                notificationService.sendRatingRequest(passenger, trip);
            }
        } else {
            log.warn("NotificationService is null, skipping rating requests");
        }

        return tripRepository.save(trip);
    }

    // Helper method to get coordinates from Point
    private double[] getCoordinatesFromPoint(Point point) {
        if (point == null) return new double[]{0, 0};
        return new double[]{point.getX(), point.getY()}; // Note: JTS uses (x=lng, y=lat)
    }

    // Method to get route from OSRM (optional)
    public String getRouteFromOSRM(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        try {
            double[] startCoords = getCoordinatesFromPoint(trip.getStartLocation());
            double[] endCoords = getCoordinatesFromPoint(trip.getEndLocation());

            // Use configuration property
            String osrmUrl = String.format(
                    "%s/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson&steps=true",
                    osrmConfig.getBaseUrl(),
                    startCoords[0], startCoords[1],  // lng, lat
                    endCoords[0], endCoords[1]       // lng, lat
            );

            // Create RestTemplate with configured timeouts
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(osrmConfig.getConnectTimeout());
            requestFactory.setReadTimeout(osrmConfig.getReadTimeout());

            RestTemplate restTemplate = new RestTemplate(requestFactory);
            return restTemplate.getForObject(osrmUrl, String.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch route from OSRM: " + e.getMessage());
        }
    }

    // Add this helper method to TripService:
    private RestTemplate createRestTemplateWithTimeout() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(osrmConfig.getConnectTimeout());
        requestFactory.setReadTimeout(osrmConfig.getReadTimeout());
        return new RestTemplate(requestFactory);
    }
}