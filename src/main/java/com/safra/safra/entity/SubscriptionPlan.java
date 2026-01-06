package com.safra.safra.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "subscription_plans")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // silver, gold, diamond, student

    @Column(nullable = false)
    private Double price; // 8, 10, 40, 5

    @Column(nullable = true)
    private Integer tripLimit; // 20, 30, NULL (unlimited), 40

    @Column(nullable = false)
    private Integer durationDays; // 30

    @Column(nullable = false)
    private Boolean requiresStudentVerification = false; // true only for student plan

    @Column(nullable = false)
    private Boolean isArchived = false;
}
