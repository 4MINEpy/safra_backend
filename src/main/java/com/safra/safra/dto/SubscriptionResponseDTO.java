package com.safra.safra.dto;

import com.safra.safra.entity.Subscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResponseDTO {
    private Long id;
    private Long userId;
    private String userName;
    private Long planId;
    private String planName;
    private Double pricePaid;
    private Integer tripLimit;
    private Integer tripsUsed;
    private Integer remainingTrips; // null means unlimited
    private Boolean isActive;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isExpired;

    public static SubscriptionResponseDTO fromEntity(Subscription subscription) {
        return SubscriptionResponseDTO.builder()
                .id(subscription.getId())
                .userId(subscription.getUser().getId())
                .userName(subscription.getUser().getName())
                .planId(subscription.getPlan().getId())
                .planName(subscription.getPlan().getName())
                .pricePaid(subscription.getPricePaid())
                .tripLimit(subscription.getTripLimit())
                .tripsUsed(subscription.getTripsUsed())
                .remainingTrips(subscription.getRemainingTrips())
                .isActive(subscription.getIsActive())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .isExpired(!subscription.isValid())
                .build();
    }
}
