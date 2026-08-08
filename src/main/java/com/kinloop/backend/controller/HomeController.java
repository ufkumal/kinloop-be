package com.kinloop.backend.controller;

import com.kinloop.backend.dto.home.HomeStatusResponse;
import com.kinloop.backend.service.CurrentParentProfileService;
import com.kinloop.backend.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;
    private final CurrentParentProfileService currentParentProfileService;

    @GetMapping("/status")
    public HomeStatusResponse status(Authentication authentication) {
        Long parentProfileId = currentParentProfileService.currentParentProfileId(authentication);
        return homeService.getStatus(parentProfileId);
    }
}
