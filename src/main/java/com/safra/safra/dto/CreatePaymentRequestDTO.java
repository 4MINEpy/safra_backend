package com.safra.safra.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentRequestDTO {
    private Long userId;
    private Long planId;
    private String successUrl;  // URL to redirect after successful payment
    private String cancelUrl;   // URL to redirect if payment cancelled
}
