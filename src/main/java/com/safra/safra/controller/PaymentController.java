package com.safra.safra.controller;

import com.safra.safra.dto.CreatePaymentRequestDTO;
import com.safra.safra.dto.PaymentResponseDTO;
import com.safra.safra.service.StripePaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class PaymentController {

    private final StripePaymentService stripePaymentService;

    // ============ PUBLIC ENDPOINTS ============

    /**
     * Get Stripe publishable key for frontend
     */
    @GetMapping("/stripe/config")
    public ResponseEntity<Map<String, String>> getStripeConfig() {
        return ResponseEntity.ok(Map.of(
                "publishableKey", stripePaymentService.getPublishableKey()
        ));
    }

    /**
     * Stripe webhook endpoint
     * This should be called by Stripe servers
     */
    @PostMapping("/stripe/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        try {
            stripePaymentService.handleWebhookEvent(payload, sigHeader);
            return ResponseEntity.ok("Webhook processed");
        } catch (SignatureVerificationException e) {
            log.error("❌ Stripe webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("❌ Stripe webhook error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }

    // ============ USER ENDPOINTS ============

    /**
     * Create a Stripe checkout session for subscription purchase
     */
    @PostMapping("/stripe/create-checkout-session")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#dto.userId)")
    public ResponseEntity<?> createCheckoutSession(@RequestBody CreatePaymentRequestDTO dto) {
        try {
            PaymentResponseDTO response = stripePaymentService.createCheckoutSession(dto);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("❌ Stripe error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Payment service error: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Verify payment status by session ID
     * Called by frontend after redirect from Stripe
     */
    @GetMapping("/stripe/verify/{sessionId}")
    public ResponseEntity<?> verifyPayment(@PathVariable String sessionId) {
        try {
            PaymentResponseDTO response = stripePaymentService.verifyPaymentBySessionId(sessionId);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("❌ Stripe verification error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Verification failed: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get payment by ID
     */
    @GetMapping("/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPayment(@PathVariable Long paymentId) {
        return stripePaymentService.getPaymentById(paymentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all payments for a user
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#userId)")
    public ResponseEntity<List<PaymentResponseDTO>> getUserPayments(@PathVariable Long userId) {
        return ResponseEntity.ok(stripePaymentService.getUserPayments(userId));
    }

    // ============ ADMIN ENDPOINTS ============

    /**
     * Get all payments (Admin)
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getAllPayments() {
        return ResponseEntity.ok(stripePaymentService.getAllPayments());
    }

    /**
     * Manually expire pending payments (Admin)
     */
    @PostMapping("/admin/expire-pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> expirePendingPayments() {
        int count = stripePaymentService.expirePendingPayments();
        return ResponseEntity.ok(Map.of(
                "message", "Expired pending payments",
                "count", count
        ));
    }
}

