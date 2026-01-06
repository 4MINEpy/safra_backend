package com.safra.safra.dto;

import lombok.Data;

@Data
public class PriceCalculationRequest {
    private double startLat;
    private double startLon;
    private double endLat;
    private double endLon;
    private String fuelType; // "essence" or "diesel"
    private int numberOfSeats;
}