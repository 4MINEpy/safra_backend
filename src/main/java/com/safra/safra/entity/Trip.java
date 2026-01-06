package com.safra.safra.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.safra.safra.serializer.PointSerializer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name="trips")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "driver_id",nullable = false)
    private User driver;

    @ManyToMany
    @JoinTable(
            name = "trip_passengers",
            joinColumns = @JoinColumn(name = "trip_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> passengers;

    @Column(columnDefinition = "geography(Point,4326)")
    @JsonSerialize(using = PointSerializer.class)
    private Point startLocation;

    @Column(columnDefinition = "geography(Point,4326)")
    @JsonSerialize(using = PointSerializer.class)
    private Point endLocation;

    @Column
    private LocalDateTime startTime;

    @Column
    private String description;

    @Column(nullable = true)
    private Boolean is_archived = false;

    @Column
    private int availableSeats;

    @Column
    private float price;

    @Column
    private String status; // Trip status field

    // Navigation fields
    @Column(name = "current_driver_lat")
    private Double currentDriverLat;

    @Column(name = "current_driver_lng")
    private Double currentDriverLng;

    @Column(name = "driver_speed")
    private Double driverSpeed;  // in km/h

    @Column(name = "driver_bearing")
    private Float driverBearing; // in degrees

    @Column(name = "last_location_update")
    private LocalDateTime lastLocationUpdate;
    @Column(name = "average_rating")
    private Double averageRating;

    @Column(name = "total_ratings")
    private Integer totalRatings = 0;
    // Status constants
    public static class Status {
        public static final String OPEN = "OPEN";
        public static final String SCHEDULED = "SCHEDULED";
        public static final String ACTIVE = "ACTIVE";
        public static final String COMPLETED = "COMPLETED";
        public static final String CANCELED = "CANCELED";
    }

    // Getter for is_archived (Lombok might not handle this well)
    public Boolean getIs_archived() {
        return is_archived;
    }

    public void setIs_archived(Boolean is_archived) {
        this.is_archived = is_archived;
    }

    // ADD THIS METHOD: Set status with validation
    public void setStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }

        String normalizedStatus = status.toUpperCase();

        // Validate against allowed statuses
        List<String> allowedStatuses = List.of(
                Status.OPEN, Status.SCHEDULED, Status.ACTIVE,
                Status.COMPLETED, Status.CANCELED
        );

        if (!allowedStatuses.contains(normalizedStatus)) {
            throw new IllegalArgumentException(
                    "Invalid status: " + status + ". Allowed values: " + allowedStatuses
            );
        }

        this.status = normalizedStatus;

        // Optional: Automatically handle some status transitions
        if (Status.COMPLETED.equals(normalizedStatus) || Status.CANCELED.equals(normalizedStatus)) {
            // Clear navigation data when trip ends
            this.currentDriverLat = null;
            this.currentDriverLng = null;
            this.driverSpeed = null;
            this.driverBearing = null;
            this.lastLocationUpdate = null;
        }
    }

    // Helper method to check if trip is active
    public boolean isActive() {
        return Status.ACTIVE.equals(this.status);
    }

    // Helper method to check if trip is completed
    public boolean isCompleted() {
        return Status.COMPLETED.equals(this.status);
    }

    // Helper method to check if trip is canceled
    public boolean isCanceled() {
        return Status.CANCELED.equals(this.status);
    }
}