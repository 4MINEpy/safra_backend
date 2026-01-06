package com.safra.safra.dto;


import lombok.Data;

@Data
public class RatingRequestDTO {
    private Long tripId;
    private Long passengerId;
    private Integer rating;
    private String comment;
}