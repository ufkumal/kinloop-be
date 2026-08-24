package com.kinloop.backend.controller;

import com.kinloop.backend.service.ChildService;
import com.kinloop.backend.dto.child.CreateChildRequest;
import com.kinloop.backend.dto.child.CreateChildResponse;
import com.kinloop.backend.dto.child.UpdateChildRequest;
import com.kinloop.backend.dto.child.UpdateChildResponse;
import com.kinloop.backend.service.CurrentParentProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/children")
@RequiredArgsConstructor
public class ChildController {

    private final ChildService childService;
    private final CurrentParentProfileService currentParentProfileService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateChildResponse create(Authentication authentication, @Valid @RequestBody CreateChildRequest request) {
        Long parentProfileId = currentParentProfileService.currentParentProfileId(authentication);
        return childService.createChild(parentProfileId, request);
    }

    @PutMapping("/{childId}")
    public UpdateChildResponse update(
            @PathVariable Long childId,
            Authentication authentication,
            @Valid @RequestBody UpdateChildRequest request) {
        Long parentProfileId = currentParentProfileService.currentParentProfileId(authentication);
        return childService.updateChild(childId, parentProfileId, request);
    }
}
