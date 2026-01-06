package com.safra.safra.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlanDTO {
    private Long id;
    private String name;
    private Double price;
    private Integer tripLimit; // null means unlimited
    private Integer durationDays;
    private Boolean requiresStudentVerification;
}
