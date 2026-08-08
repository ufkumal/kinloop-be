package com.kinloop.backend.controller;

import com.kinloop.backend.dto.consent.ConsentResponse;
import com.kinloop.backend.dto.consent.UpdateConsentRequest;
import com.kinloop.backend.entity.User;
import com.kinloop.backend.service.ConsentService;
import com.kinloop.backend.service.CurrentUserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/consents")
@RequiredArgsConstructor
public class ConsentController {
    private final ConsentService consentService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<ConsentResponse> activeConsents(Authentication authentication) {
        User user = currentUserService.currentUser(authentication);
        return consentService.getActiveConsents(user.getId());
    }

    @PutMapping("/{consentId}")
    public ConsentResponse updateConsent(
            Authentication authentication,
            @PathVariable Long consentId,
            @Valid @RequestBody UpdateConsentRequest request) {
        User user = currentUserService.currentUser(authentication);
        return consentService.updateConsent(user, consentId, request.granted());
    }
}
