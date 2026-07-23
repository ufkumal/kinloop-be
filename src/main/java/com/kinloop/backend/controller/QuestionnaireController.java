package com.kinloop.backend.controller;

import com.kinloop.backend.dto.questionnaire.*;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/children/{childId}/questionnaire")
@RequiredArgsConstructor
public class QuestionnaireController {
    private final CurrentParentProfileService currentParentProfileService;
    private final ChildService childService;
    private final QuestionnaireSessionService questionnaireSessionService;

    @GetMapping("/current")
    public CurrentQuestionnaireResponse current(@PathVariable Long childId, Authentication authentication) {
        return questionnaireSessionService.current(ownedChild(childId, authentication));
    }

    @PutMapping("/answers/{questionCode}")
    public CurrentQuestionnaireResponse answer(@PathVariable Long childId, @PathVariable String questionCode,
            @Valid @RequestBody SubmitAnswerRequest request, Authentication authentication) {
        return questionnaireSessionService.answer(ownedChild(childId, authentication), questionCode, request.optionCode());
    }

    @PostMapping("/complete")
    public QuestionnaireCompleteResponse complete(@PathVariable Long childId, Authentication authentication) {
        return questionnaireSessionService.complete(ownedChild(childId, authentication));
    }

    private Child ownedChild(Long childId, Authentication authentication) {
        Long parentId = currentParentProfileService.currentParentProfileId(authentication);
        return childService.getOwnedChild(childId, parentId);
    }
}
