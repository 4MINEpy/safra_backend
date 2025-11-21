package com.safra.safra.service;


import com.safra.safra.dto.TripRequestDTO;
import com.safra.safra.entity.Trip;
import com.safra.safra.entity.User;
import com.safra.safra.repository.TripRepository;
import com.safra.safra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TripService {

    @Autowired
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }
    public Optional<Trip> getTripById(Long id) {
        return tripRepository.findById(id);
    }
    public void createTrip(TripRequestDTO dto) {
        Trip trip = new Trip();

        // Set driver
        trip.setDriver(userRepository.findById(dto.getDriverId())
                .orElseThrow(() -> new RuntimeException("Driver not found")));

        // Set passengers
        List<User> passengers = userRepository.findAllById(dto.getPassengerIds());
        trip.setPassengers(passengers);

        // Convert coordinates to Point
        GeometryFactory geometryFactory = new GeometryFactory();
        Point start = geometryFactory.createPoint(new Coordinate(dto.getStartLocation().getX(), dto.getStartLocation().getY()));
        Point end = geometryFactory.createPoint(new Coordinate(dto.getEndLocation().getX(), dto.getEndLocation().getY()));
        trip.setStartLocation(start);
        trip.setEndLocation(end);

        // Set other fields
        trip.setStartTime(dto.getStartTime());
        trip.setDescription(dto.getDescription());
        trip.setAvailableSeats(dto.getAvailableSeats());
        trip.setPrice(dto.getPrice());
        trip.setStatus(dto.getStatus());

        // Save trip
         tripRepository.save(trip);
    }
    public void deleteTrip(Long id) {tripRepository.deleteById(id);}

    public void updateTrip(Trip trip) {
        tripRepository.save(trip);
    }
}
