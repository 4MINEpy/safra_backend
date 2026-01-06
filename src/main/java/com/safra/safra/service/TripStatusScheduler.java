package com.safra.safra.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TripStatusScheduler {

    @PersistenceContext
    private EntityManager entityManager;

    @Scheduled(cron = "0 * * * * *") // runs every minute
    @Transactional
    public void cancelExpiredTrips() {
        // 15-minute grace period
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        int updated = entityManager.createQuery(
                "UPDATE Trip t SET t.status = 'CANCELLED' " +
                        "WHERE t.status = 'OPEN' AND t.startTime < :cutoff"
        ).setParameter("cutoff",cutoff).executeUpdate();

        System.out.println("Cancelled " + updated + " trips past grace period at " + LocalDateTime.now());
    }

}
