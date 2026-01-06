package com.safra.safra.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "stripe_payments")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stripe identifiers
    @Column(unique = true)
    private String stripeSessionId;         // Checkout session ID (cs_xxx)

    @Column
    private String stripePaymentIntentId;   // Payment intent ID (pi_xxx)

    @Column
    private String stripeCustomerId;        // Customer ID if created (cus_xxx)

    // User and subscription info
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = true)
    private Subscription subscription;      // Created after successful payment

    // Payment details
    @Column(nullable = false)
    private Double amount;                  // Amount in TND

    @Column(nullable = false)
    private String currency;                // "tnd"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    // URLs
    @Column(length = 500)
    private String checkoutUrl;             // Stripe checkout URL

    @Column(length = 500)
    private String successUrl;

    @Column(length = 500)
    private String cancelUrl;

    // Metadata
    @Column
    private String paymentMethod;           // card, etc.

    @Column
    private String receiptEmail;

    @Column(length = 500)
    private String receiptUrl;              // Stripe receipt URL

    @Column(length = 1000)
    private String failureMessage;          // Error message if failed

    // Timestamps
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private LocalDateTime expiresAt;        // Session expiration

    @Column(nullable = false)
    private Boolean isArchived = false;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }
}
