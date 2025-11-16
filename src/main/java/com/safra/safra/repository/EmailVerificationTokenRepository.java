package com.safra.safra.repository;

import com.safra.safra.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);

    @Query("SELECT t FROM EmailVerificationToken t WHERE t.user.email = :email")
    Optional<EmailVerificationToken> findByUserEmail(String email);
}