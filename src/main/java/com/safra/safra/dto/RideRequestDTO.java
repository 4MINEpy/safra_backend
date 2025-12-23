package com.safra.safra.dto;

import com.safra.safra.entity.RideRequest;
import com.safra.safra.entity.Trip;
import com.safra.safra.entity.User;
import lombok.Data;

@Data
public class RideRequestDTO {
    private Long tripId;
    private Long passengerId;
    private String comment;

    public RideRequest toEntity(Trip trip, User passenger) {
        return RideRequest.builder()
                .trip(trip)
                .passenger(passenger)
                .comment(comment)
                .build();
    }
}

