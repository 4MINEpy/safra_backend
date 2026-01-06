package com.safra.safra.config;

import com.safra.safra.entity.SubscriptionPlan;
import com.safra.safra.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes default subscription plans on application startup
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPlanInitializer implements CommandLineRunner {

    private final SubscriptionPlanRepository planRepository;

    @Override
    public void run(String... args) {
        // Only initialize if no plans exist
        if (planRepository.count() > 0) {
            log.info("📋 Subscription plans already exist, skipping initialization");
            return;
        }

        log.info("📋 Initializing default subscription plans...");

        // Silver Plan - 20 trips for 8 DT
        SubscriptionPlan silver = SubscriptionPlan.builder()
                .name("silver")
                .price(8.0)
                .tripLimit(20)
                .durationDays(30)
                .requiresStudentVerification(false)
                .isArchived(false)
                .build();

        // Gold Plan - 30 trips for 10 DT
        SubscriptionPlan gold = SubscriptionPlan.builder()
                .name("gold")
                .price(10.0)
                .tripLimit(30)
                .durationDays(30)
                .requiresStudentVerification(false)
                .isArchived(false)
                .build();

        // Diamond Plan - Unlimited trips for 40 DT
        SubscriptionPlan diamond = SubscriptionPlan.builder()
                .name("diamond")
                .price(40.0)
                .tripLimit(null)  // NULL = unlimited
                .durationDays(30)
                .requiresStudentVerification(false)
                .isArchived(false)
                .build();

        // Student Plan - 40 trips for 5 DT (requires student verification)
        SubscriptionPlan student = SubscriptionPlan.builder()
                .name("student")
                .price(5.0)
                .tripLimit(40)
                .durationDays(30)
                .requiresStudentVerification(true)
                .isArchived(false)
                .build();

        planRepository.save(silver);
        planRepository.save(gold);
        planRepository.save(diamond);
        planRepository.save(student);

        log.info("✅ Default subscription plans created:");
        log.info("   - Silver: 20 trips / 8 DT");
        log.info("   - Gold: 30 trips / 10 DT");
        log.info("   - Diamond: Unlimited trips / 40 DT");
        log.info("   - Student: 40 trips / 5 DT (requires student email verification)");
    }
}
