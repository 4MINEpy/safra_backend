package com.safra.safra.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class FuelPriceCalculationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Tunisia fuel prices (TND per liter) - Updated periodically
    private static final double ESSENCE_PRICE_PER_LITER = 2.350; // Essence sans plomb
    private static final double DIESEL_PRICE_PER_LITER = 2.160;   // Gasoil

    // Average consumption rates (liters per 100km)
    private static final double ESSENCE_CONSUMPTION_PER_100KM = 7.0;
    private static final double DIESEL_CONSUMPTION_PER_100KM = 5.5;

    /**
     * Calculate trip price based on distance and fuel type
     * @param distanceKm Distance in kilometers
     * @param fuelType "essence" or "diesel"
     * @return Calculated price in TND
     */
    public double calculateTripPrice(double distanceKm, String fuelType) {
        double pricePerLiter;
        double consumptionPer100Km;

        if ("diesel".equalsIgnoreCase(fuelType)) {
            pricePerLiter = DIESEL_PRICE_PER_LITER;
            consumptionPer100Km = DIESEL_CONSUMPTION_PER_100KM;
        } else {
            // Default to essence
            pricePerLiter = ESSENCE_PRICE_PER_LITER;
            consumptionPer100Km = ESSENCE_CONSUMPTION_PER_100KM;
        }

        // Calculate fuel consumption for the distance
        double fuelNeeded = (distanceKm / 100.0) * consumptionPer100Km;

        // Calculate base fuel cost
        double fuelCost = fuelNeeded * pricePerLiter;

        // Add margin for carpooling (driver profit + maintenance)
        double marginMultiplier = 1.3; // 30% markup

        double totalPrice = fuelCost * marginMultiplier;

        // Round to 2 decimal places
        return Math.round(totalPrice * 100.0) / 100.0;
    }

    /**
     * Calculate price per passenger (divide by number of seats)
     * @param distanceKm Distance in kilometers
     * @param fuelType "essence" or "diesel"
     * @param numberOfSeats Number of available seats
     * @return Price per passenger in TND
     */
    public double calculatePricePerPassenger(double distanceKm, String fuelType, int numberOfSeats) {
        double totalPrice = calculateTripPrice(distanceKm, fuelType);
        double pricePerPassenger = totalPrice / numberOfSeats;

        // Round to 2 decimal places
        return Math.round(pricePerPassenger * 100.0) / 100.0;
    }

    /**
     * Get distance between two coordinates using OSRM
     * @param startLat Starting latitude
     * @param startLon Starting longitude
     * @param endLat Ending latitude
     * @param endLon Ending longitude
     * @return Distance in kilometers
     */
    public double calculateDistance(double startLat, double startLon,
                                    double endLat, double endLon) {
        try {
            String osrmUrl = String.format(
                    "http://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=false",
                    startLon, startLat, endLon, endLat
            );

            ResponseEntity<String> response = restTemplate.getForEntity(osrmUrl, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            // Get distance in meters from OSRM response
            double distanceMeters = root.path("routes").get(0).path("distance").asDouble();

            // Convert to kilometers
            return distanceMeters / 1000.0;

        } catch (Exception e) {
            // Fallback to Haversine formula if OSRM fails
            return calculateHaversineDistance(startLat, startLon, endLat, endLon);
        }
    }

    /**
     * Calculate distance using Haversine formula (as fallback)
     */
    private double calculateHaversineDistance(double lat1, double lon1,
                                              double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance in km
    }

    /**
     * Get suggested price range for a trip
     */
    public PriceRange getSuggestedPriceRange(double distanceKm, String fuelType, int seats) {
        double calculatedPrice = calculatePricePerPassenger(distanceKm, fuelType, seats);

        // Suggest a range: -20% to +20%
        double minPrice = Math.round(calculatedPrice * 0.8 * 100.0) / 100.0;
        double maxPrice = Math.round(calculatedPrice * 1.2 * 100.0) / 100.0;

        return new PriceRange(calculatedPrice, minPrice, maxPrice);
    }

    public static class PriceRange {
        public final double suggested;
        public final double min;
        public final double max;

        public PriceRange(double suggested, double min, double max) {
            this.suggested = suggested;
            this.min = min;
            this.max = max;
        }
    }
}

