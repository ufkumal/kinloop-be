package com.kinloop.backend.controller;

import com.kinloop.backend.service.OnboardingService;
import com.kinloop.backend.dto.onboarding.IdentityQuestionsResponse;
import com.kinloop.backend.dto.onboarding.DailyTimeBudgetResponse;
import com.kinloop.backend.dto.onboarding.UpdateDailyTimeBudgetRequest;
import com.kinloop.backend.service.CurrentParentProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final CurrentParentProfileService currentParentProfileService;

    @GetMapping("/identity-questions")
    public IdentityQuestionsResponse identityQuestions(Authentication authentication) {
        Long parentProfileId = currentParentProfileService.currentParentProfileId(authentication);
        return onboardingService.getIdentityQuestions(parentProfileId);
    }

    @PutMapping("/daily-time-budget")
    public DailyTimeBudgetResponse updateDailyTimeBudget(
            Authentication authentication,
            @Valid @RequestBody UpdateDailyTimeBudgetRequest request) {
        Long parentProfileId = currentParentProfileService.currentParentProfileId(authentication);
        return onboardingService.updateDailyTimeBudget(parentProfileId, request.optionCode());
    }
}
