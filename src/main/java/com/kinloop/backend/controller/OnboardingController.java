package com.kinloop.backend.controller;

import com.kinloop.backend.service.OnboardingService;
import com.kinloop.backend.dto.onboarding.IdentityQuestionsResponse;
import com.kinloop.backend.dto.onboarding.DailyTimeBudgetResponse;
import com.kinloop.backend.dto.onboarding.UpdateDailyTimeBudgetRequest;
import com.kinloop.backend.dto.onboarding.OnboardingClosingMessageResponse;
import com.kinloop.backend.dto.onboarding.UpdateOnboardingClosingMessageRequest;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.service.ChildService;
import com.kinloop.backend.service.CurrentParentProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final ChildService childService;

    @GetMapping("/identity-questions")
    public IdentityQuestionsResponse identityQuestions(Authentication authentication) {
        currentParentProfileService.currentParentProfileId(authentication);
        return onboardingService.getIdentityQuestions();
    }

    @GetMapping("/children/{childId}/daily-time-budget")
    public com.kinloop.backend.dto.profile.DailyTimeBudgetProfileResponse dailyTimeBudget(
            @PathVariable Long childId,
            Authentication authentication) {
        return onboardingService.getDailyTimeBudget(ownedChild(childId, authentication));
    }

    @PutMapping("/children/{childId}/daily-time-budget")
    public DailyTimeBudgetResponse updateDailyTimeBudget(
            @PathVariable Long childId,
            Authentication authentication,
            @Valid @RequestBody UpdateDailyTimeBudgetRequest request) {
        return onboardingService.updateDailyTimeBudget(
                ownedChild(childId, authentication), request.optionCode());
    }

    @GetMapping("/children/{childId}/closing-message")
    public OnboardingClosingMessageResponse closingMessage(
            @PathVariable Long childId,
            Authentication authentication) {
        return onboardingService.getClosingMessage(ownedChild(childId, authentication));
    }

    @PutMapping("/children/{childId}/closing-message")
    public OnboardingClosingMessageResponse updateClosingMessage(
            @PathVariable Long childId,
            Authentication authentication,
            @Valid @RequestBody UpdateOnboardingClosingMessageRequest request) {
        return onboardingService.respondToClosingMessage(
                ownedChild(childId, authentication), request.action());
    }

    private Child ownedChild(Long childId, Authentication authentication) {
        Long parentProfileId = currentParentProfileService.currentParentProfileId(authentication);
        return childService.getOwnedChild(childId, parentProfileId);
    }
}
