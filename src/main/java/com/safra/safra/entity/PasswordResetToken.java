package com.safra.safra.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // store hashed code
    @Column(nullable = false)
    private String codeHash;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private Integer attemptsRemaining = 3;

    @Column(nullable = false)
    private boolean consumed = false;

    public PasswordResetToken(String codeHash, User user, LocalDateTime expiryDate, Integer attemptsRemaining) {
        this.codeHash = codeHash;
        this.user = user;
        this.expiryDate = expiryDate;
        this.attemptsRemaining = attemptsRemaining;
        this.consumed = false;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}
