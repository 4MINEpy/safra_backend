package com.safra.safra.dto;

import com.safra.safra.entity.PaymentStatus;
import com.safra.safra.entity.StripePayment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDTO {
    private Long id;
    private String stripeSessionId;
    private String checkoutUrl;
    private Long userId;
    private String userName;
    private Long planId;
    private String planName;
    private Double amount;
    private String currency;
    private PaymentStatus status;
    private String receiptUrl;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Long subscriptionId;

    public static PaymentResponseDTO fromEntity(StripePayment payment) {
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .stripeSessionId(payment.getStripeSessionId())
                .checkoutUrl(payment.getCheckoutUrl())
                .userId(payment.getUser().getId())
                .userName(payment.getUser().getName())
                .planId(payment.getPlan().getId())
                .planName(payment.getPlan().getName())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .receiptUrl(payment.getReceiptUrl())
                .createdAt(payment.getCreatedAt())
                .completedAt(payment.getCompletedAt())
                .subscriptionId(payment.getSubscription() != null ? payment.getSubscription().getId() : null)
                .build();
    }
}
