package com.safra.safra.service;

import com.safra.safra.dto.SubscriptionPurchaseDTO;
import com.safra.safra.dto.SubscriptionResponseDTO;
import com.safra.safra.entity.Subscription;
import com.safra.safra.entity.SubscriptionPlan;
import com.safra.safra.entity.User;
import com.safra.safra.repository.SubscriptionPlanRepository;
import com.safra.safra.repository.SubscriptionRepository;
import com.safra.safra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserRepository userRepository;

    /**
     * Get all available (non-archived) subscription plans
     */
    public List<SubscriptionPlan> getAllActivePlans() {
        return planRepository.findByIsArchivedFalse();
    }

    /**
     * Get plan by ID
     */
    public Optional<SubscriptionPlan> getPlanById(Long planId) {
        return planRepository.findByIdAndIsArchivedFalse(planId);
    }

    /**
     * Purchase a subscription for a user
     */
    @Transactional
    public SubscriptionResponseDTO purchaseSubscription(SubscriptionPurchaseDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        SubscriptionPlan plan = planRepository.findByIdAndIsArchivedFalse(dto.getPlanId())
                .orElseThrow(() -> new RuntimeException("Subscription plan not found or archived"));

        // Check if plan requires student verification
        if (plan.getRequiresStudentVerification()) {
            if (!user.getStudentVerified()) {
                throw new RuntimeException("Student verification required for this plan. Please verify your student email first.");
            }
        }

        // Check if user already has an active subscription
        Optional<Subscription> existingActive = subscriptionRepository.findActiveSubscription(
                user.getId(), LocalDateTime.now());

        if (existingActive.isPresent()) {
            throw new RuntimeException("You already have an active subscription. Please wait for it to expire or cancel it first.");
        }

        // Create new subscription
        LocalDateTime now = LocalDateTime.now();
        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .pricePaid(plan.getPrice())
                .tripLimit(plan.getTripLimit())
                .tripsUsed(0)
                .isActive(true)
                .startDate(now)
                .endDate(now.plusDays(plan.getDurationDays()))
                .isArchived(false)
                .build();

        subscription = subscriptionRepository.save(subscription);
        log.info("✅ Subscription purchased: User {} bought {} plan", user.getEmail(), plan.getName());

        return SubscriptionResponseDTO.fromEntity(subscription);
    }

    /**
     * Get user's active subscription
     */
    public Optional<SubscriptionResponseDTO> getActiveSubscription(Long userId) {
        return subscriptionRepository.findActiveSubscription(userId, LocalDateTime.now())
                .map(SubscriptionResponseDTO::fromEntity);
    }

    /**
     * Get all subscriptions for a user (history)
     */
    public List<SubscriptionResponseDTO> getUserSubscriptionHistory(Long userId) {
        return subscriptionRepository.findByUserId(userId).stream()
                .map(SubscriptionResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Check if user can create a trip (has valid subscription with remaining trips)
     */
    public boolean canUserCreateTrip(Long userId) {
        Optional<Subscription> activeSubscription = subscriptionRepository.findActiveSubscription(
                userId, LocalDateTime.now());

        if (activeSubscription.isEmpty()) {
            return false;
        }

        Subscription subscription = activeSubscription.get();
        return subscription.isValid() && subscription.hasRemainingTrips();
    }

    /**
     * Use a trip from user's subscription (called when creating a trip)
     * Returns true if successful, throws exception otherwise
     */
    @Transactional
    public void useTrip(Long userId) {
        Subscription subscription = subscriptionRepository.findActiveSubscription(userId, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("No active subscription found. Please purchase a subscription to create trips."));

        if (!subscription.isValid()) {
            throw new RuntimeException("Your subscription has expired. Please renew to create trips.");
        }

        if (!subscription.hasRemainingTrips()) {
            throw new RuntimeException("You have used all trips in your subscription. Please upgrade or wait for renewal.");
        }

        subscription.useTrip();
        subscriptionRepository.save(subscription);
        log.info("📍 Trip used: User {} now has {} trips remaining",
                userId, subscription.getRemainingTrips() == null ? "unlimited" : subscription.getRemainingTrips());
    }

    /**
     * Cancel a subscription
     */
    @Transactional
    public SubscriptionResponseDTO cancelSubscription(Long subscriptionId, Long userId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (!subscription.getUser().getId().equals(userId)) {
            throw new RuntimeException("You can only cancel your own subscription");
        }

        if (!subscription.getIsActive()) {
            throw new RuntimeException("Subscription is already cancelled");
        }

        subscription.setIsActive(false);
        subscription = subscriptionRepository.save(subscription);
        log.info("❌ Subscription cancelled: User {} cancelled subscription {}", userId, subscriptionId);

        return SubscriptionResponseDTO.fromEntity(subscription);
    }

    /**
     * Get subscription status summary for a user
     */
    public SubscriptionStatusDTO getSubscriptionStatus(Long userId) {
        Optional<Subscription> activeSubscription = subscriptionRepository.findActiveSubscription(
                userId, LocalDateTime.now());

        if (activeSubscription.isEmpty()) {
            return SubscriptionStatusDTO.builder()
                    .hasActiveSubscription(false)
                    .canCreateTrip(false)
                    .message("No active subscription. Purchase a plan to create trips.")
                    .build();
        }

        Subscription sub = activeSubscription.get();
        boolean canCreate = sub.hasRemainingTrips();

        return SubscriptionStatusDTO.builder()
                .hasActiveSubscription(true)
                .canCreateTrip(canCreate)
                .subscriptionId(sub.getId())
                .planName(sub.getPlan().getName())
                .tripsUsed(sub.getTripsUsed())
                .tripLimit(sub.getTripLimit())
                .remainingTrips(sub.getRemainingTrips())
                .expiresAt(sub.getEndDate())
                .message(canCreate ? "You can create trips." :
                        "Trip limit reached. Please upgrade your plan.")
                .build();
    }

    /**
     * Deactivate expired subscriptions (can be called by a scheduled task)
     */
    @Transactional
    public int deactivateExpiredSubscriptions() {
        List<Subscription> expired = subscriptionRepository.findExpiredActiveSubscriptions(LocalDateTime.now());
        for (Subscription sub : expired) {
            sub.setIsActive(false);
            subscriptionRepository.save(sub);
            log.info("⏰ Subscription expired: User {} subscription {} deactivated",
                    sub.getUser().getId(), sub.getId());
        }
        return expired.size();
    }

    // ============ ADMIN METHODS ============

    /**
     * Create a new subscription plan (Admin)
     */
    @Transactional
    public SubscriptionPlan createPlan(SubscriptionPlan plan) {
        if (planRepository.existsByName(plan.getName())) {
            throw new RuntimeException("Plan with this name already exists");
        }
        return planRepository.save(plan);
    }

    /**
     * Update subscription plan (Admin)
     */
    @Transactional
    public SubscriptionPlan updatePlan(Long planId, SubscriptionPlan updatedPlan) {
        SubscriptionPlan existing = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        existing.setName(updatedPlan.getName());
        existing.setPrice(updatedPlan.getPrice());
        existing.setTripLimit(updatedPlan.getTripLimit());
        existing.setDurationDays(updatedPlan.getDurationDays());
        existing.setRequiresStudentVerification(updatedPlan.getRequiresStudentVerification());

        return planRepository.save(existing);
    }

    /**
     * Archive a plan (Admin) - soft delete
     */
    @Transactional
    public SubscriptionPlan archivePlan(Long planId) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));
        plan.setIsArchived(true);
        return planRepository.save(plan);
    }

    /**
     * Get all subscriptions (Admin)
     */
    public List<SubscriptionResponseDTO> getAllSubscriptions() {
        return subscriptionRepository.findAll().stream()
                .map(SubscriptionResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Inner DTO for status
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SubscriptionStatusDTO {
        private boolean hasActiveSubscription;
        private boolean canCreateTrip;
        private Long subscriptionId;
        private String planName;
        private Integer tripsUsed;
        private Integer tripLimit;
        private Integer remainingTrips;
        private LocalDateTime expiresAt;
        private String message;
    }
}
