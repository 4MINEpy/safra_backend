package com.safra.safra.repository;

import com.safra.safra.entity.Trip;
import org.locationtech.jts.triangulate.tri.Tri;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByDriverId(Long driverId);
}
