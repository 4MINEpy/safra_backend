package com.safra.safra.security;

import com.safra.safra.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {

    private final UserRepository userRepository;

    public UserSecurity(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isSelf(Long id) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findById(id)
                .map(user -> user.getEmail().equals(currentEmail))
                .orElse(false);
    }
}
