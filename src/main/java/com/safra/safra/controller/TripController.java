package com.safra.safra.controller;

import com.safra.safra.dto.TripRequestDTO;
import com.safra.safra.entity.Trip;
import com.safra.safra.service.TripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class TripController {
    @Autowired
    TripService tripService;

    @RequestMapping(value = "/trips", method = RequestMethod.GET)
    public List<Trip> getAllTrips() {
        return tripService.getAllTrips();
    }
    @RequestMapping(value = "/trips/{id}", method = RequestMethod.GET)
    public Optional<Trip> getTrip(@PathVariable Long id) {
        return tripService.getTripById(id);
    }
    @RequestMapping(value = "/trips",method = RequestMethod.POST)
    public void createTrip(@RequestBody TripRequestDTO dto) {
        tripService.createTrip(dto);
    }
    @RequestMapping(value = "/trips/{id}",method = RequestMethod.DELETE)
    public void deleteTrip(@PathVariable Long id) {
        tripService.deleteTrip(id);
    }
    @PutMapping("/trips")
    public ResponseEntity<?> updateTrip(@RequestBody TripRequestDTO dto) {
        Trip updated = tripService.updateTrip(dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("trips/{tripId}/passengers")
    public ResponseEntity<?> getPassengers(@PathVariable Long tripId) {
        return ResponseEntity.ok(tripService.getPassengersForTrip(tripId));
    }

    @DeleteMapping("trips/{tripId}/passengers/{passengerId}")
    public ResponseEntity<?> removePassenger(
            @PathVariable Long tripId,
            @PathVariable Long passengerId
    ) {
        tripService.removePassengerFromTrip(tripId, passengerId);
        return ResponseEntity.ok("Passenger removed successfully");
    }
    @PutMapping("trips/{tripId}/cancel")
    public ResponseEntity<?> cancelTrip(
            @PathVariable Long tripId,
            @RequestParam Long driverId
    ) {
        Trip trip = tripService.cancelTrip(tripId, driverId);
        return ResponseEntity.ok(trip);
    }
    @PutMapping("trips/{tripId}/leave")
    public ResponseEntity<?> leaveTrip(
            @PathVariable Long tripId,
            @RequestParam Long passengerId
    ) {
        Trip trip = tripService.removePassenger(tripId, passengerId);
        return ResponseEntity.ok(trip);
    }



}
