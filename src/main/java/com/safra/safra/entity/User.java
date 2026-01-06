package com.safra.safra.entity;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Data
@Table(name = "users", indexes = {@Index(name = "idx_user_email", columnList = "email")})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String name;
    @Column(unique = true)
    private String email;
    @Column
    private String phoneNumber;
    @Column
    private String password;
    @Column(nullable = true)
    private String profilePicture;
    @Column
    @Enumerated(EnumType.STRING)
    private Gender gender;
    @Column
    private LocalDate birthDate;
    @Column
    @Enumerated(EnumType.STRING)
    private Role role;
    @Column
    private Boolean isBanned;
    @Column
    private LocalDateTime joinDate;
    @Column(name = "average_rating")
    private Double averageRating;

    @Column(name = "total_ratings")
    private Integer totalRatings = 0;
    @Column(name = "fcm_token")
    private String fcmToken;
    public Boolean isIs_archived() {
        return is_archived;
    }

    public void setIs_archived(boolean is_archived) {
        this.is_archived = is_archived;
    }

    @Column(nullable = true)
    private Boolean is_archived=false;
    @Column(nullable = false)
    private boolean isEmailVerified = false;

    @OneToOne(mappedBy = "owner",cascade = CascadeType.ALL)
    @JsonManagedReference
    private Car car;

    @Column(unique = true,nullable = true)
    private String studentEmail;
    @Column
    private Boolean studentVerified =false;
}
