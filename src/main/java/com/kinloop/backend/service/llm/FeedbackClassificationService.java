package com.kinloop.backend.service.llm;

import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.ParentProfile;
import com.kinloop.backend.entity.enums.ConsentType;
import com.kinloop.backend.entity.enums.FeedbackType;
import com.kinloop.backend.repository.ChildRepository;
import com.kinloop.backend.repository.ParentProfileRepository;
import com.kinloop.backend.service.ConsentService;
import com.kinloop.backend.service.matching.MatchingParameters;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Performs the provider call synchronously and never lets an LLM failure escape to the API. */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackClassificationService {
    private final ChildRepository childRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final ConsentService consentService;
    private final LlmProperties llmProperties;
    private final AnthropicMessagesClient client;
    private final FeedbackClassificationParser parser;
    private final MatchingParameters matchingParameters;
    private final SecondhandReportDetector secondhandReportDetector;

    public FeedbackClassificationOutcome classify(
            Long childId,
            Activity activity,
            FeedbackType feedbackType,
            String freeText
    ) {
        if (freeText == null || !llmProperties.isEnabled()) {
            return FeedbackClassificationOutcome.notAttempted();
        }

        try {
            if (!hasDataProcessingConsent(childId)) {
                return FeedbackClassificationOutcome.notAttempted();
            }

            String rawResponse = client.complete(
                    FeedbackClassificationPrompt.SYSTEM_PROMPT,
                    FeedbackClassificationPrompt.buildUserMessage(activity, feedbackType, freeText));
            ParsedClassification parsed = parser.parse(rawResponse);

            if (parsed.valid() && secondhandReportDetector.isSecondhand(freeText)) {
                BigDecimal cap = matchingParameters.load().get("llm_secondhand_confidence_cap");
                if (cap == null) {
                    throw new IllegalStateException("Missing scoring parameter: llm_secondhand_confidence_cap");
                }
                if (parsed.confidence().compareTo(cap) > 0) {
                    parsed = parsed.withConfidence(cap);
                }
            }

            return FeedbackClassificationOutcome.completed(
                    llmProperties.getModel(), rawResponse, parsed);
        } catch (RuntimeException e) {
            // Fail open: the caller continues with ordinary button learning and the API
            // response contains no provider/internal error details.
            log.warn("LLM classification failed for child {}: {}", childId, e.getMessage());
            return FeedbackClassificationOutcome.notAttempted();
        }
    }

    private boolean hasDataProcessingConsent(Long childId) {
        Child child = childRepository.findById(childId).orElse(null);
        if (child == null) return false;
        ParentProfile parentProfile = parentProfileRepository.findById(child.getParentId()).orElse(null);
        if (parentProfile == null) return false;
        return consentService.hasGrantedConsent(parentProfile.getUserId(), ConsentType.DATA_PROCESSING);
    }
}
