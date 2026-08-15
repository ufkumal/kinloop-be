package com.kinloop.backend.controller;

import com.kinloop.backend.dto.matching.DailyPlanResponse;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.service.ActivityMatchingService;
import com.kinloop.backend.service.ChildService;
import com.kinloop.backend.service.CurrentParentProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/children/{childId}/daily-plan")
@RequiredArgsConstructor
public class DailyPlanController {
    private final CurrentParentProfileService currentParentProfileService;
    private final ChildService childService;
    private final ActivityMatchingService matchingService;

    @GetMapping("/today")
    public DailyPlanResponse today(@PathVariable Long childId, Authentication authentication) {
        Long parentId = currentParentProfileService.currentParentProfileId(authentication);
        Child child = childService.getOwnedChild(childId, parentId);
        return matchingService.today(child);
    }
}
