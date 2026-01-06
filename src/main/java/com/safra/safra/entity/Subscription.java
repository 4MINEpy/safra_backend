package com.safra.safra.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "subscriptions")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Column(nullable = false)
    private Double pricePaid; // snapshot of price at purchase time

    @Column(nullable = true)
    private Integer tripLimit; // snapshot of trip limit (NULL = unlimited)

    @Column(nullable = false)
    private Integer tripsUsed = 0;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Boolean isArchived = false;

    /**
     * Check if subscription has remaining trips
     * Returns true if unlimited (tripLimit is null) or if trips used < limit
     */
    public boolean hasRemainingTrips() {
        if (tripLimit == null) {
            return true; // unlimited
        }
        return tripsUsed < tripLimit;
    }

    /**
     * Check if subscription is currently valid (active and not expired)
     */
    public boolean isValid() {
        return isActive && !isArchived && LocalDateTime.now().isBefore(endDate);
    }

    /**
     * Get remaining trips count (null means unlimited)
     */
    public Integer getRemainingTrips() {
        if (tripLimit == null) {
            return null; // unlimited
        }
        return Math.max(0, tripLimit - tripsUsed);
    }

    /**
     * Increment trips used counter
     */
    public void useTrip() {
        this.tripsUsed++;
    }
}
