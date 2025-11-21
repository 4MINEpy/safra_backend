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
    @Column(nullable = false)
    private boolean isEmailVerified = false;

    @OneToOne(mappedBy = "owner",cascade = CascadeType.ALL)
    @JsonManagedReference
    private Car car;

}
