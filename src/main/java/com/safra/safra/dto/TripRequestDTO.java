package com.safra.safra.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;



@Data
public class TripRequestDTO {
    @Data
    public static class LocationDTO {
        private double x;
        private double y;
    }
    private Long id;
    private Long driverId;
    private List<Long> passengerIds;
    private LocationDTO startLocation;
    private LocationDTO endLocation;
    private LocalDateTime startTime;
    private String description;
    private int availableSeats;
    private float price;
    private String status;
}


