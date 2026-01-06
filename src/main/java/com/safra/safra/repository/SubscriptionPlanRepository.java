package com.safra.safra.repository;

import com.safra.safra.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    Optional<SubscriptionPlan> findByName(String name);

    List<SubscriptionPlan> findByIsArchivedFalse();

    Optional<SubscriptionPlan> findByIdAndIsArchivedFalse(Long id);

    boolean existsByName(String name);
}
