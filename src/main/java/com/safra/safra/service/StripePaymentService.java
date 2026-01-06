package com.safra.safra.service;

import com.safra.safra.dto.CreatePaymentRequestDTO;
import com.safra.safra.dto.PaymentResponseDTO;
import com.safra.safra.dto.SubscriptionPurchaseDTO;
import com.safra.safra.entity.*;
import com.safra.safra.repository.StripePaymentRepository;
import com.safra.safra.repository.SubscriptionPlanRepository;
import com.safra.safra.repository.UserRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentService {

    private final StripePaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionService subscriptionService;

    @Value("${stripe.currency:usd}")
    private String currency;

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    @Value("${stripe.api.publishable-key}")
    private String publishableKey;

    /**
     * Get Stripe publishable key for frontend
     */
    public String getPublishableKey() {
        return publishableKey;
    }

    /**
     * Create a Stripe Checkout Session for subscription purchase
     */
    @Transactional
    public PaymentResponseDTO createCheckoutSession(CreatePaymentRequestDTO dto) throws StripeException {
        // Validate user
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate plan
        SubscriptionPlan plan = planRepository.findByIdAndIsArchivedFalse(dto.getPlanId())
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));

        // Check if plan requires student verification
        if (plan.getRequiresStudentVerification() && !user.getStudentVerified()) {
            throw new RuntimeException("Student verification required for this plan");
        }

        // Check if user already has active subscription
        if (subscriptionService.canUserCreateTrip(user.getId())) {
            throw new RuntimeException("You already have an active subscription");
        }

        // Convert price to cents (Stripe uses smallest currency unit)
        // For USD, multiply by 100 (1 USD = 100 cents)
        long amountInSmallestUnit = Math.round(plan.getPrice() * 100);

        // Build checkout session
        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(user.getEmail())
                .setClientReferenceId(user.getId().toString())
                .setSuccessUrl(dto.getSuccessUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(dto.getCancelUrl())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(currency)
                                                .setUnitAmount(amountInSmallestUnit)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("SAFRA " + capitalize(plan.getName()) + " Subscription")
                                                                .setDescription(buildPlanDescription(plan))
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .putMetadata("user_id", user.getId().toString())
                .putMetadata("plan_id", plan.getId().toString())
                .putMetadata("plan_name", plan.getName());

        // Create session
        Session session = Session.create(paramsBuilder.build());

        // Save payment record
        StripePayment payment = StripePayment.builder()
                .stripeSessionId(session.getId())
                .user(user)
                .plan(plan)
                .amount(plan.getPrice())
                .currency(currency)
                .status(PaymentStatus.PENDING)
                .checkoutUrl(session.getUrl())
                .successUrl(dto.getSuccessUrl())
                .cancelUrl(dto.getCancelUrl())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(30)) // Sessions expire in 30 min
                .isArchived(false)
                .build();

        payment = paymentRepository.save(payment);
        log.info("✅ Stripe checkout session created: {} for user: {}", session.getId(), user.getEmail());

        return PaymentResponseDTO.fromEntity(payment);
    }

    /**
     * Handle Stripe webhook events
     */
    @Transactional
    public void handleWebhookEvent(String payload, String sigHeader) throws SignatureVerificationException {
        Event event;

        if (webhookSecret != null && !webhookSecret.isEmpty()) {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } else {
            // For testing without webhook secret
            Gson gson = new Gson();
            event = gson.fromJson(payload, Event.class);
        }

        log.info("📩 Stripe webhook event: {}", event.getType());

        switch (event.getType()) {
            case "checkout.session.completed":
                handleCheckoutSessionCompleted(event);
                break;
            case "checkout.session.expired":
                handleCheckoutSessionExpired(event);
                break;
            case "payment_intent.payment_failed":
                handlePaymentFailed(event);
                break;
            default:
                log.info("Unhandled event type: {}", event.getType());
        }
    }

    /**
     * Handle successful checkout completion
     */
    private void handleCheckoutSessionCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Failed to deserialize session"));

        Optional<StripePayment> paymentOpt = paymentRepository.findByStripeSessionId(session.getId());

        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for session: {}", session.getId());
            return;
        }

        StripePayment payment = paymentOpt.get();

        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            log.info("Payment already processed: {}", session.getId());
            return;
        }

        // Update payment record
        payment.setStripePaymentIntentId(session.getPaymentIntent());
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setCompletedAt(LocalDateTime.now());
        payment.setReceiptEmail(session.getCustomerEmail());

        // Create subscription for user
        try {
            SubscriptionPurchaseDTO purchaseDTO = new SubscriptionPurchaseDTO();
            purchaseDTO.setUserId(payment.getUser().getId());
            purchaseDTO.setPlanId(payment.getPlan().getId());

            var subscriptionResponse = subscriptionService.purchaseSubscription(purchaseDTO);
            
            // Link subscription to payment (need to fetch the actual subscription entity)
            // For now, we just mark payment as complete
            log.info("✅ Subscription created for user {} after payment {}", 
                    payment.getUser().getEmail(), session.getId());

        } catch (Exception e) {
            log.error("❌ Failed to create subscription after payment: {}", e.getMessage());
            payment.setFailureMessage("Payment succeeded but subscription creation failed: " + e.getMessage());
        }

        paymentRepository.save(payment);
    }

    /**
     * Handle expired checkout session
     */
    private void handleCheckoutSessionExpired(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Failed to deserialize session"));

        paymentRepository.findByStripeSessionId(session.getId()).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(payment);
            log.info("⏰ Payment session expired: {}", session.getId());
        });
    }

    /**
     * Handle failed payment
     */
    private void handlePaymentFailed(Event event) {
        // Extract payment intent ID and update record
        log.warn("❌ Payment failed event received");
    }

    /**
     * Verify payment status by session ID (for frontend verification)
     */
    @Transactional
    public PaymentResponseDTO verifyPaymentBySessionId(String sessionId) throws StripeException {
        // Retrieve session from Stripe
        Session session = Session.retrieve(sessionId);

        Optional<StripePayment> paymentOpt = paymentRepository.findByStripeSessionId(sessionId);

        if (paymentOpt.isEmpty()) {
            throw new RuntimeException("Payment not found");
        }

        StripePayment payment = paymentOpt.get();

        // Update status based on Stripe session
        if ("complete".equals(session.getStatus()) && "paid".equals(session.getPaymentStatus())) {
            if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
                payment.setStatus(PaymentStatus.SUCCEEDED);
                payment.setStripePaymentIntentId(session.getPaymentIntent());
                payment.setCompletedAt(LocalDateTime.now());

                // Create subscription if not already created
                if (payment.getSubscription() == null) {
                    try {
                        SubscriptionPurchaseDTO purchaseDTO = new SubscriptionPurchaseDTO();
                        purchaseDTO.setUserId(payment.getUser().getId());
                        purchaseDTO.setPlanId(payment.getPlan().getId());
                        subscriptionService.purchaseSubscription(purchaseDTO);
                        log.info("✅ Subscription created after payment verification");
                    } catch (Exception e) {
                        log.warn("Subscription may already exist: {}", e.getMessage());
                    }
                }

                paymentRepository.save(payment);
            }
        } else if ("expired".equals(session.getStatus())) {
            payment.setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(payment);
        }

        return PaymentResponseDTO.fromEntity(payment);
    }

    /**
     * Get payment by ID
     */
    public Optional<PaymentResponseDTO> getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .map(PaymentResponseDTO::fromEntity);
    }

    /**
     * Get all payments for a user
     */
    public List<PaymentResponseDTO> getUserPayments(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(PaymentResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all payments (Admin)
     */
    public List<PaymentResponseDTO> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(PaymentResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Expire pending payments that have passed expiration time
     */
    @Transactional
    public int expirePendingPayments() {
        List<StripePayment> expired = paymentRepository.findExpiredPendingPayments(LocalDateTime.now());
        for (StripePayment payment : expired) {
            payment.setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(payment);
        }
        return expired.size();
    }

    // Helper methods
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private String buildPlanDescription(SubscriptionPlan plan) {
        String trips = plan.getTripLimit() == null ? "Unlimited" : plan.getTripLimit().toString();
        return trips + " trips for " + plan.getDurationDays() + " days";
    }
}
