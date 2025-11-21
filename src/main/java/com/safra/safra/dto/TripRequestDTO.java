package com.safra.safra.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TripRequestDTO {
    private Long driverId;
    private List<Long> passengerIds;
    private double startX;
    private double startY;
    private double endX;
    private double endY;
    private LocalDateTime startTime;
    private String description;
    private int availableSeats;
    private float price;
    private String status;
}
