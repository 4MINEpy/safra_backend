package com.safra.safra.repository;

import com.safra.safra.entity.PaymentStatus;
import com.safra.safra.entity.StripePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StripePaymentRepository extends JpaRepository<StripePayment, Long> {

    Optional<StripePayment> findByStripeSessionId(String sessionId);

    Optional<StripePayment> findByStripePaymentIntentId(String paymentIntentId);

    List<StripePayment> findByUserId(Long userId);

    List<StripePayment> findByUserIdAndStatus(Long userId, PaymentStatus status);

    List<StripePayment> findByStatus(PaymentStatus status);

    @Query("SELECT p FROM StripePayment p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    List<StripePayment> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT p FROM StripePayment p WHERE p.status = 'PENDING' AND p.expiresAt < :now")
    List<StripePayment> findExpiredPendingPayments(@Param("now") LocalDateTime now);

    @Query("SELECT p FROM StripePayment p WHERE p.user.id = :userId AND p.status = 'SUCCEEDED' ORDER BY p.completedAt DESC")
    List<StripePayment> findSuccessfulPaymentsByUser(@Param("userId") Long userId);

    boolean existsByUserIdAndPlanIdAndStatus(Long userId, Long planId, PaymentStatus status);
}
