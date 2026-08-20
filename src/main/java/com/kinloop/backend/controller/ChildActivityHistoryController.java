package com.kinloop.backend.controller;

import com.kinloop.backend.dto.profile.ActivityHistoryResponse;
import com.kinloop.backend.service.ActivityHistoryService;
import com.kinloop.backend.service.ChildService;
import com.kinloop.backend.service.CurrentParentProfileService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/children/{childId}/activity-history")
@RequiredArgsConstructor
public class ChildActivityHistoryController {

    private final CurrentParentProfileService currentParentProfileService;
    private final ChildService childService;
    private final ActivityHistoryService activityHistoryService;

    @GetMapping
    public ActivityHistoryResponse activityHistory(
            @PathVariable Long childId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication authentication) {
        Long parentProfileId = currentParentProfileService.currentParentProfileId(authentication);
        childService.getOwnedChild(childId, parentProfileId);
        return activityHistoryService.getSelectedHistory(childId, page, size);
    }
}
