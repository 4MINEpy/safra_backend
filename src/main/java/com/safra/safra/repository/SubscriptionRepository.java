package com.safra.safra.repository;

import com.safra.safra.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserId(Long userId);

    List<Subscription> findByUserIdAndIsActiveTrue(Long userId);

    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.isActive = true AND s.isArchived = false AND s.endDate > :now")
    Optional<Subscription> findActiveSubscription(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.isActive = true AND s.isArchived = false AND s.endDate > :now ORDER BY s.endDate DESC")
    List<Subscription> findAllActiveSubscriptions(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM Subscription s WHERE s.isActive = true AND s.endDate < :now")
    List<Subscription> findExpiredActiveSubscriptions(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(s) > 0 FROM Subscription s WHERE s.user.id = :userId AND s.isActive = true AND s.isArchived = false AND s.endDate > :now")
    boolean hasActiveSubscription(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
