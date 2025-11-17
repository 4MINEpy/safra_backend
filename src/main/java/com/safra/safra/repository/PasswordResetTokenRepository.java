package com.safra.safra.repository;

import com.safra.safra.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @Query("SELECT t FROM PasswordResetToken t WHERE t.user.email = :email")
    Optional<PasswordResetToken> findByUserEmail(String email);

    void deleteByUserId(Long userId);
}
