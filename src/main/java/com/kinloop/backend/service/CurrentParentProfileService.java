package com.kinloop.backend.service;

import com.kinloop.backend.entity.ParentProfile;
import com.kinloop.backend.entity.User;
import com.kinloop.backend.entity.enums.UserRole;
import com.kinloop.backend.repository.ParentProfileRepository;
import com.kinloop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentParentProfileService {

    private final UserRepository userRepository;
    private final ParentProfileRepository parentProfileRepository;

    @Transactional(readOnly = true)
    public Long currentParentProfileId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
        if (user.getRole() != UserRole.PARENT) {
            throw new AccessDeniedException("Parent account required");
        }
        ParentProfile parentProfile = parentProfileRepository.findByUserIdAndDeletedAtIsNull(user.getId())
                .orElseThrow(() -> new IllegalStateException("Parent profile not found"));
        return parentProfile.getId();
    }
}
