package com.safra.safra.service;

import com.safra.safra.entity.RequestStatus;
import com.safra.safra.entity.RideRequest;
import com.safra.safra.entity.Trip;
import com.safra.safra.entity.User;
import com.safra.safra.repository.RideRequestRepository;
import com.safra.safra.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RideRequestService {

    private final RideRequestRepository rideRequestRepository;
    private final TripRepository tripRepository;

    public RideRequest createRequest(RideRequest rideRequest) {
        rideRequest.setStatus(RequestStatus.PENDING);
        rideRequest.setCreatedAt(LocalDateTime.now());
        rideRequest.setUpdatedAt(LocalDateTime.now());
        return rideRequestRepository.save(rideRequest);
    }

    public RideRequest acceptRideRequest(Long requestId) {
        RideRequest request = rideRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Ride request not found"));

        Trip trip = request.getTrip();
        User passenger = request.getPassenger();

        if (trip.getAvailableSeats() <= 0) {
            throw new RuntimeException("No available seats");
        }

        // Update ride request status
        request.setStatus(RequestStatus.ACCEPTED);
        request.setUpdatedAt(LocalDateTime.now());

        // Decrease seats
        trip.setAvailableSeats(trip.getAvailableSeats() - 1);

        // Add passenger to trip_passengers join table
        trip.getPassengers().add(passenger);

        // Persist changes
        tripRepository.save(trip);
        rideRequestRepository.save(request);

        return request;
    }


    public RideRequest rejectRideRequest(Long requestId) {
        RideRequest request = rideRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Ride request not found"));

        request.setStatus(RequestStatus.REJECTED);
        request.setUpdatedAt(LocalDateTime.now());

        return rideRequestRepository.save(request);
    }
    public List<RideRequest> getRequestsForDriver(Long driverId) {
        return rideRequestRepository.findByTrip_Driver_Id(driverId);
    }
    public List<RideRequest> getRequestsForPassenger(Long passengerId) {
        return rideRequestRepository.findByPassenger_Id(passengerId);
    }

}

