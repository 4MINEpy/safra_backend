package com.safra.safra.service;

import com.safra.safra.entity.PasswordResetToken;
import com.safra.safra.entity.User;
import com.safra.safra.repository.PasswordResetTokenRepository;
import com.safra.safra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // Request a password reset. Returns true if a user with the email exists (and an email was sent).
    public boolean requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();

        // remove existing tokens for the user
        tokenRepository.findByUserEmail(email).ifPresent(tokenRepository::delete);

        // generate a 6-digit code
        String code = String.format("%06d", (int) (Math.random() * 1_000_000));

        String codeHash = passwordEncoder.encode(code);
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);
        PasswordResetToken token = new PasswordResetToken(codeHash, user, expiry, 3);
        tokenRepository.save(token);

        // send email
        emailService.sendPasswordResetEmail(user.getEmail(), code);

        return true;
    }

    // Verify code and reset password. Returns true if successful.
    public boolean verifyCodeAndReset(String email, String code, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByUserEmail(email);
        if (tokenOpt.isEmpty()) return false;

        PasswordResetToken token = tokenOpt.get();

        if (token.isExpired() || token.isConsumed() || token.getAttemptsRemaining() <= 0) {
            tokenRepository.delete(token);
            return false;
        }

        boolean matches = passwordEncoder.matches(code, token.getCodeHash());
        if (!matches) {
            token.setAttemptsRemaining(token.getAttemptsRemaining() - 1);
            tokenRepository.save(token);
            if (token.getAttemptsRemaining() <= 0) tokenRepository.delete(token);
            return false;
        }

        // match: reset password
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // consume token
        tokenRepository.delete(token);
        return true;
    }
}
