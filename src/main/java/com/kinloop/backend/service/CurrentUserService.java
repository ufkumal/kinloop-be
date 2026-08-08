package com.kinloop.backend.service;

import com.kinloop.backend.entity.User;
import com.kinloop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserRepository userRepository;

    public User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        return userRepository.findByEmail(authentication.getName())
                .filter(user -> user.getDeletedAt() == null && user.isActive())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
    }
}
