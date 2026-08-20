package com.kinloop.backend.controller;

import com.kinloop.backend.dto.profile.DailyTimeBudgetProfileResponse;
import com.kinloop.backend.dto.profile.ProfileResponse;
import com.kinloop.backend.service.CurrentParentProfileService;
import com.kinloop.backend.service.OnboardingService;
import com.kinloop.backend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final CurrentParentProfileService currentParentProfileService;
    private final ProfileService profileService;
    private final OnboardingService onboardingService;

    @GetMapping
    public ProfileResponse profile(Authentication authentication) {
        Long parentProfileId = currentParentProfileService.currentParentProfileId(authentication);
        return profileService.getProfile(parentProfileId);
    }

    @GetMapping("/daily-time-budget")
    public DailyTimeBudgetProfileResponse dailyTimeBudget(Authentication authentication) {
        Long parentProfileId = currentParentProfileService.currentParentProfileId(authentication);
        return onboardingService.getDailyTimeBudget(parentProfileId);
    }
}
