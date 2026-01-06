package com.safra.safra.controller;

import com.safra.safra.dto.CreatePaymentRequestDTO;
import com.safra.safra.dto.PaymentResponseDTO;
import com.safra.safra.dto.SubscriptionPurchaseDTO;
import com.safra.safra.dto.SubscriptionResponseDTO;
import com.safra.safra.entity.SubscriptionPlan;
import com.safra.safra.service.StripePaymentService;
import com.safra.safra.service.SubscriptionService;
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
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final StripePaymentService stripePaymentService;

    // ============ PUBLIC ENDPOINTS ============

    /**
     * Get all available subscription plans
     */
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getAvailablePlans() {
        return ResponseEntity.ok(subscriptionService.getAllActivePlans());
    }

    /**
     * Get a specific plan by ID
     */
    @GetMapping("/plans/{planId}")
    public ResponseEntity<?> getPlanById(@PathVariable Long planId) {
        return subscriptionService.getPlanById(planId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ============ USER ENDPOINTS ============

    /**
     * Purchase a subscription via Stripe payment
     * Returns a Stripe checkout URL for the user to complete payment
     */
    @PostMapping("/purchase")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#dto.userId)")
    public ResponseEntity<?> purchaseSubscription(@RequestBody CreatePaymentRequestDTO dto) {
        try {
            // Validate required fields
            if (dto.getUserId() == null || dto.getPlanId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId and planId are required"));
            }
            
            // Set default URLs if not provided
            if (dto.getSuccessUrl() == null || dto.getSuccessUrl().isEmpty()) {
                dto.setSuccessUrl("http://localhost:4200/payment-success");
            }
            if (dto.getCancelUrl() == null || dto.getCancelUrl().isEmpty()) {
                dto.setCancelUrl("http://localhost:4200/payment-cancel");
            }

            // Create Stripe checkout session
            PaymentResponseDTO response = stripePaymentService.createCheckoutSession(dto);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Payment session created. Redirect user to checkout URL.",
                    "checkoutUrl", response.getCheckoutUrl(),
                    "sessionId", response.getStripeSessionId(),
                    "paymentId", response.getId(),
                    "amount", response.getAmount(),
                    "currency", response.getCurrency(),
                    "planName", response.getPlanName()
            ));
        } catch (StripeException e) {
            log.error("❌ Stripe error during purchase: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Payment service error: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get user's active subscription
     */
    @GetMapping("/user/{userId}/active")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#userId)")
    public ResponseEntity<?> getActiveSubscription(@PathVariable Long userId) {
        var subscription = subscriptionService.getActiveSubscription(userId);
        if (subscription.isPresent()) {
            return ResponseEntity.ok(subscription.get());
        }
        return ResponseEntity.ok(Map.of("message", "No active subscription"));
    }

    /**
     * Get user's subscription history
     */
    @GetMapping("/user/{userId}/history")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#userId)")
    public ResponseEntity<List<SubscriptionResponseDTO>> getSubscriptionHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.getUserSubscriptionHistory(userId));
    }

    /**
     * Get user's subscription status (can they create trips?)
     */
    @GetMapping("/user/{userId}/status")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#userId)")
    public ResponseEntity<SubscriptionService.SubscriptionStatusDTO> getSubscriptionStatus(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionStatus(userId));
    }

    /**
     * Check if user can create a trip
     */
    @GetMapping("/user/{userId}/can-create-trip")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#userId)")
    public ResponseEntity<Map<String, Boolean>> canCreateTrip(@PathVariable Long userId) {
        boolean canCreate = subscriptionService.canUserCreateTrip(userId);
        return ResponseEntity.ok(Map.of("canCreateTrip", canCreate));
    }

    /**
     * Cancel subscription
     */
    @PostMapping("/{subscriptionId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelSubscription(
            @PathVariable Long subscriptionId,
            @RequestParam Long userId) {
        try {
            SubscriptionResponseDTO response = subscriptionService.cancelSubscription(subscriptionId, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ ADMIN ENDPOINTS ============

    /**
     * Create a new subscription plan (Admin)
     */
    @PostMapping("/admin/plans")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPlan(@RequestBody SubscriptionPlan plan) {
        try {
            SubscriptionPlan created = subscriptionService.createPlan(plan);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update subscription plan (Admin)
     */
    @PutMapping("/admin/plans/{planId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePlan(@PathVariable Long planId, @RequestBody SubscriptionPlan plan) {
        try {
            SubscriptionPlan updated = subscriptionService.updatePlan(planId, plan);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Archive a plan (Admin)
     */
    @DeleteMapping("/admin/plans/{planId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> archivePlan(@PathVariable Long planId) {
        try {
            SubscriptionPlan archived = subscriptionService.archivePlan(planId);
            return ResponseEntity.ok(archived);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all subscriptions (Admin)
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SubscriptionResponseDTO>> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions());
    }

    /**
     * Manually grant a subscription to a user without payment (Admin only)
     * Use this for promotions, refunds, or special cases
     */
    @PostMapping("/admin/grant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> grantSubscription(@RequestBody SubscriptionPurchaseDTO dto) {
        try {
            SubscriptionResponseDTO response = subscriptionService.purchaseSubscription(dto);
            log.info("✅ Admin granted subscription to user {} for plan {}", dto.getUserId(), dto.getPlanId());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Subscription granted successfully",
                    "subscription", response
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Manually deactivate expired subscriptions (Admin)
     */
    @PostMapping("/admin/deactivate-expired")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deactivateExpired() {
        int count = subscriptionService.deactivateExpiredSubscriptions();
        return ResponseEntity.ok(Map.of(
                "message", "Deactivated expired subscriptions",
                "count", count
        ));
    }
}
