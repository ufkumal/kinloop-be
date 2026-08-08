package com.kinloop.backend.service;

import com.kinloop.backend.dto.home.HomeStatusResponse;
import com.kinloop.backend.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final ChildRepository childRepository;

    @Transactional(readOnly = true)
    public HomeStatusResponse getStatus(Long parentProfileId) {
        boolean onboardingCompleted =
                childRepository.existsByParentIdAndOnboardingCompletedAtIsNotNullAndDeletedAtIsNull(parentProfileId);
        return onboardingCompleted ? HomeStatusResponse.returningUser() : HomeStatusResponse.newUser();
    }
}
