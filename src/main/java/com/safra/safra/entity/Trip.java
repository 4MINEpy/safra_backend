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
    @GeneratedValue
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

    public Boolean isIs_archived() {
        return is_archived;
    }

    public void setIs_archived(boolean is_archived) {
        this.is_archived = is_archived;
    }

    @Column(nullable = true)
    private Boolean is_archived=false;
    @Column
    private int availableSeats;

    @Column
    private float price;

    @Column
    private String status;
}
