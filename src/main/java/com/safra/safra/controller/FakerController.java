package com.safra.safra.controller;

import com.safra.safra.service.FakerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 🎲 SAFRA Faker Controller
 * 
 * Generate realistic test data for the carpooling application.
 * ⚠️ WARNING: This will CLEAR existing data! Use only in development.
 * 
 * Endpoints:
 *   POST /api/faker/populate           - Full database population with defaults
 *   POST /api/faker/populate/custom    - Custom population with parameters
 *   GET  /api/faker/stats              - Get current database statistics
 *   DELETE /api/faker/clear            - Clear all data (dangerous!)
 */
@RestController
@RequestMapping("/api/faker")
@RequiredArgsConstructor
@Slf4j
public class FakerController {

    private final FakerService fakerService;

    /**
     * 🚀 Populate database with default settings
     * Creates 100 users, 3-8 trips per driver
     */
    @PostMapping("/populate")
    public ResponseEntity<Map<String, Object>> populateDatabase() {
        log.info("🎲 Faker endpoint called - populating database with defaults...");
        
        Map<String, Object> result = fakerService.populateDatabase(100, 5);
        
        if ("SUCCESS".equals(result.get("status"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 🎯 Populate database with custom settings
     * 
     * @param userCount Number of users to create (default: 100, max: 1000)
     * @param tripsPerDriver Max trips per driver (default: 5, max: 20)
     */
    @PostMapping("/populate/custom")
    public ResponseEntity<Map<String, Object>> populateDatabaseCustom(
            @RequestParam(defaultValue = "100") int userCount,
            @RequestParam(defaultValue = "5") int tripsPerDriver) {
        
        log.info("🎲 Faker endpoint called - custom population: {} users, {} trips/driver", 
                userCount, tripsPerDriver);
        
        // Validate parameters
        if (userCount < 1) userCount = 1;
        if (userCount > 1000) userCount = 1000;
        if (tripsPerDriver < 1) tripsPerDriver = 1;
        if (tripsPerDriver > 20) tripsPerDriver = 20;
        
        Map<String, Object> result = fakerService.populateDatabase(userCount, tripsPerDriver);
        
        if ("SUCCESS".equals(result.get("status"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 📊 Get current database statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getDatabaseStats() {
        log.info("📊 Retrieving database statistics...");
        return ResponseEntity.ok(fakerService.getDatabaseStats());
    }

    /**
     * 🎲 Quick populate - minimal data for quick testing
     * Creates 20 users, 2 trips per driver
     */
    @PostMapping("/populate/quick")
    public ResponseEntity<Map<String, Object>> quickPopulate() {
        log.info("🎲 Quick faker population...");
        Map<String, Object> result = fakerService.populateDatabase(20, 2);
        
        if ("SUCCESS".equals(result.get("status"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 🎲 Large populate - lots of data for stress testing
     * Creates 500 users, 10 trips per driver
     */
    @PostMapping("/populate/large")
    public ResponseEntity<Map<String, Object>> largePopulate() {
        log.info("🎲 Large faker population - this may take a while...");
        Map<String, Object> result = fakerService.populateDatabase(500, 10);
        
        if ("SUCCESS".equals(result.get("status"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.internalServerError().body(result);
        }
    }
}
