package com.safra.safra.repository;

import com.safra.safra.entity.RideRequest;
import com.safra.safra.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideRequestRepository extends JpaRepository<RideRequest, Long> {

    List<RideRequest> findByTrip_Id(Long tripId);
    List<RideRequest> findByTrip_Driver_Id(Long tripId);
    List<RideRequest> findByPassenger_Id(Long passengerId);
}
