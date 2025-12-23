package com.safra.safra.service;


import com.safra.safra.dto.TripRequestDTO;
import com.safra.safra.entity.RequestStatus;
import com.safra.safra.entity.RideRequest;
import com.safra.safra.entity.Trip;
import com.safra.safra.entity.User;
import com.safra.safra.repository.RideRequestRepository;
import com.safra.safra.repository.TripRepository;
import com.safra.safra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TripService {

    @Autowired
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final RideRequestRepository rideRequestRepository;

    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    public Optional<Trip> getTripById(Long id) {
        return tripRepository.findById(id);
    }

    // FIXED: Return Trip instead of void
    public Trip createTrip(TripRequestDTO dto) {
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

        // FIXED: Return the saved trip
        return tripRepository.save(trip);
    }

    public void deleteTrip(Long id) {
        tripRepository.deleteById(id);
    }

    // FIXED: Return Trip instead of void and accept ID parameter
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


    // ADD THIS METHOD: For archiving trips
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

        // Remove all passengers
        trip.getPassengers().clear();

        // Cancel all requests linked to this trip
        List<RideRequest> requests = rideRequestRepository.findByTrip_Id(tripId);
        for (RideRequest req : requests) {
            req.setStatus(RequestStatus.CANCELLED);
            req.setUpdatedAt(LocalDateTime.now());
        }

        rideRequestRepository.saveAll(requests);
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



}
