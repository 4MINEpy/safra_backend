package com.safra.safra.controller;

import com.safra.safra.dto.TripRequestDTO;
import com.safra.safra.dto.DriverLocationUpdateDTO;
import com.safra.safra.dto.PriceCalculationRequest;

import com.safra.safra.entity.Trip;
import com.safra.safra.service.FuelPriceCalculationService;
import com.safra.safra.service.TripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class TripController {
    @Autowired
    TripService tripService;
    @Autowired  // ← ADD THIS ANNOTATION
    FuelPriceCalculationService fuelPriceService;

    @RequestMapping(value = "/trips", method = RequestMethod.GET)
    public List<Trip> getAllTrips() {
        return tripService.getAllTrips();
    }

    @RequestMapping(value = "/trips/{id}", method = RequestMethod.GET)
    public Optional<Trip> getTrip(@PathVariable Long id) {
        return tripService.getTripById(id);
    }

    @RequestMapping(value = "/trips", method = RequestMethod.POST)

    public ResponseEntity<?> createTrip(@RequestBody TripRequestDTO dto) {
        try {
            // If price not provided, calculate it
            if (dto.getPrice() == null || dto.getPrice() == 0) {
                double distance = fuelPriceService.calculateDistance(
                        dto.getStartY(), dto.getStartX(),
                        dto.getEndY(), dto.getEndX()
                );

                // Get fuel type from driver's car
                String fuelType = dto.getFuelType() != null ? dto.getFuelType() : "essence";

                double calculatedPrice = fuelPriceService.calculatePricePerPassenger(
                        distance,
                        fuelType,
                        dto.getAvailableSeats()
                );

                dto.setPrice((float) calculatedPrice);
            }

            Trip trip = tripService.createTrip(dto);
            return ResponseEntity.ok(trip);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @RequestMapping(value = "/trips/{id}", method = RequestMethod.DELETE)
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

    @RequestMapping(value = "/trips/search/user", method = RequestMethod.GET)
    public List<Trip> getSearchedTripsByUser(
            @RequestParam("departureLat") double departureLat,
            @RequestParam("departureLng") double departureLng,
            @RequestParam("destinationLat") double destinationLat,
            @RequestParam("destinationLng") double destinationLng) {

        List<Trip> trips = tripService.findTripsWithinDistance(
                departureLat, departureLng,
                destinationLat, destinationLng);

        return trips;
    }

    @PutMapping("/{tripId}/status")
    public ResponseEntity<?> updateTripStatus(
            @PathVariable Long tripId,
            @RequestParam String status) {
        try {
            Trip updatedTrip = tripService.updateTripStatus(tripId, status);
            return ResponseEntity.ok(updatedTrip);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update driver's current location
     * PUT /api/trips/{tripId}/location
     */
    @PutMapping("/{tripId}/location")
    public ResponseEntity<?> updateDriverLocation(
            @PathVariable Long tripId,
            @RequestBody DriverLocationUpdateDTO locationUpdate) {
        try {
            Trip updatedTrip = tripService.updateDriverLocation(
                    tripId,
                    locationUpdate.getLatitude(),
                    locationUpdate.getLongitude(),
                    locationUpdate.getSpeed(),
                    locationUpdate.getBearing(),
                    locationUpdate.getAccuracy()
            );
            return ResponseEntity.ok(updatedTrip);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get driver's current location (for passengers)
     * GET /api/trips/{tripId}/location
     */
    @GetMapping("/{tripId}/location")
    public ResponseEntity<?> getDriverLocation(@PathVariable Long tripId) {
        try {
            DriverLocationUpdateDTO location = tripService.getDriverLocation(tripId);
            return ResponseEntity.ok(location);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Start navigation
     * POST /api/trips/{tripId}/start-navigation
     */
    @PostMapping("/{tripId}/start-navigation")
    public ResponseEntity<?> startNavigation(@PathVariable Long tripId) {
        try {
            Trip updatedTrip = tripService.startNavigation(tripId);
            return ResponseEntity.ok(updatedTrip);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * End navigation
     * POST /api/trips/{tripId}/end-navigation
     */
    @PostMapping("/{tripId}/end-navigation")
    public ResponseEntity<?> endNavigation(@PathVariable Long tripId) {
        try {
            Trip updatedTrip = tripService.endNavigation(tripId);
            return ResponseEntity.ok(updatedTrip);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get trip route from OSRM (optional)
     * GET /api/trips/{tripId}/route
     */
    @GetMapping("/{tripId}/route")
    public ResponseEntity<?> getTripRoute(@PathVariable Long tripId) {
        try {
            String route = tripService.getRouteFromOSRM(tripId);
            return ResponseEntity.ok(route);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Find active trips for tracking
     * GET /api/trips/active
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveTrips() {
        try {
            // You might want to add a repository method to find active trips
            List<Trip> allTrips = tripService.getAllTrips();
            List<Trip> activeTrips = allTrips.stream()
                    .filter(trip -> "ACTIVE".equals(trip.getStatus()))
                    .filter(trip -> trip.getCurrentDriverLat() != null)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(activeTrips);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/trips/calculate-price")
    public ResponseEntity<?> calculatePrice(@RequestBody PriceCalculationRequest request) {
        try {
            // Get distance from OSRM
            double distance = fuelPriceService.calculateDistance(
                    request.getStartLat(),
                    request.getStartLon(),
                    request.getEndLat(),
                    request.getEndLon()
            );

            // Calculate price range
            FuelPriceCalculationService.PriceRange priceRange =
                    fuelPriceService.getSuggestedPriceRange(
                            distance,
                            request.getFuelType(),
                            request.getNumberOfSeats()
                    );

            return ResponseEntity.ok(Map.of(
                    "distance", Math.round(distance * 100.0) / 100.0,
                    "suggestedPrice", priceRange.suggested,
                    "minPrice", priceRange.min,
                    "maxPrice", priceRange.max,
                    "fuelType", request.getFuelType()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}




