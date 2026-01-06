package com.safra.safra.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DriverLocationUpdateDTO {
    private Double latitude;
    private Double longitude;
    private Double speed;      // optional - in km/h
    private Float bearing;     // optional - in degrees
    private Float accuracy;    // optional - in meters
    private LocalDateTime timestamp = LocalDateTime.now();
}