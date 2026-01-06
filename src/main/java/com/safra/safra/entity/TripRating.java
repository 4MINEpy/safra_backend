package com.safra.safra.entity;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a rating given by a passenger for a completed trip
 */
@Entity
@Data
@Table(
        name = "trip_ratings",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"trip_id", "passenger_id"})
        },
        indexes = {
                @Index(name = "idx_trip_ratings_trip", columnList = "trip_id"),
                @Index(name = "idx_trip_ratings_passenger", columnList = "passenger_id"),
                @Index(name = "idx_trip_ratings_created", columnList = "created_at")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    @JsonBackReference
    private Trip trip;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    @Column(nullable = false)
    private Integer rating; // 1-5 stars

    @Column(length = 500)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Automatically set creation timestamp before persisting
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        // Validate rating range
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
    }

    /**
     * Validate rating before update
     */
    @PreUpdate
    protected void onUpdate() {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
    }
}