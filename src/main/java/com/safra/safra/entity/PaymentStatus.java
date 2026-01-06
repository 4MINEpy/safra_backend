package com.safra.safra.entity;

public enum PaymentStatus {
    PENDING,        // Payment initiated but not completed
    SUCCEEDED,      // Payment successful
    FAILED,         // Payment failed
    CANCELLED,      // Payment cancelled by user
    REFUNDED,       // Payment was refunded
    EXPIRED         // Payment session expired
}
