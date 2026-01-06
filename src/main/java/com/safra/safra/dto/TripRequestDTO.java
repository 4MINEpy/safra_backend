package com.safra.safra.dto;

import com.safra.safra.entity.Trip;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TripRequestDTO {
    private Long id;
    private Long driverId;
    private List<Long> passengerIds;
    private double startX;
    private double startY;
    private double endX;
    private double endY;
    private LocalDateTime startTime;
    private String description;
    private int availableSeats;
    private String status;
    private Float price; // Changed to Float (nullable)
    private String fuelType;
}


