package com.kinloop.backend.service.llm;

import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.Feedback;
import com.kinloop.backend.entity.FeedbackLlmClassification;
import com.kinloop.backend.entity.ParentProfile;
import com.kinloop.backend.entity.enums.ConsentType;
import com.kinloop.backend.entity.enums.SituationHint;
import com.kinloop.backend.repository.ChildRepository;
import com.kinloop.backend.repository.FeedbackLlmClassificationRepository;
import com.kinloop.backend.repository.FeedbackRepository;
import com.kinloop.backend.repository.ParentProfileRepository;
import com.kinloop.backend.service.ConsentService;
import com.kinloop.backend.service.FeedbackLearningService;
import com.kinloop.backend.service.matching.MatchingParameters;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates one free-text classification: calls the model, validates and stores the
 * result, then applies it through FeedbackLearningService's existing, tested hooks.
 * Runs after the triggering vote's transaction has committed (see
 * FeedbackClassificationEventListener) — reverseEffects()/reapplyGardnerLearningWithOverride()
 * operate on a button credit that is already durably written.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackClassificationService {
    private final FeedbackRepository feedbackRepository;
    private final FeedbackLlmClassificationRepository classificationRepository;
    private final ChildRepository childRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final ConsentService consentService;
    private final LlmProperties llmProperties;
    private final AnthropicMessagesClient client;
    private final FeedbackClassificationParser parser;
    private final MatchingParameters matchingParameters;
    private final FeedbackLearningService feedbackLearningService;

    @Transactional
    public void classifyAndApply(Long feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId).orElse(null);
        if (feedback == null || feedback.getFreeText() == null) {
            return;
        }
        if (!llmProperties.isEnabled()) {
            return;
        }
        if (!hasDataProcessingConsent(feedback.getChildId())) {
            return;
        }

        String rawResponse;
        try {
            rawResponse = client.complete(
                    FeedbackClassificationPrompt.SYSTEM_PROMPT,
                    FeedbackClassificationPrompt.buildUserMessage(
                            feedback.getActivity(), feedback.getFeedbackType(), feedback.getFreeText()));
        } catch (RuntimeException e) {
            log.warn("LLM classification call failed for feedback {}: {}", feedbackId, e.getMessage());
            return;
        }

        ParsedClassification parsed = parser.parse(rawResponse);
        FeedbackLlmClassification classification = new FeedbackLlmClassification(
                feedback, llmProperties.getModel(), rawResponse,
                parsed.confidence(), parsed.targetCorrection(), parsed.secondaryHint(),
                parsed.sensoryHint(), parsed.involvementHint(), parsed.difficultyHint(),
                parsed.situationHint(), parsed.durationHint(), parsed.conflict());

        if (!parsed.valid()) {
            classificationRepository.save(classification);
            return;
        }

        BigDecimal threshold = matchingParameters.load().get("llm_feedback_confidence_threshold");
        if (parsed.confidence().compareTo(threshold) < 0) {
            classificationRepository.save(classification);
            return;
        }

        if (parsed.conflict()) {
            log.warn("LLM_CONFLICT feedback={}", feedbackId);
        }

        // TRANSIENT carries no information about the child's interest: the button's own
        // credit (already written synchronously by submit()) is undone, not reinforced.
        if (parsed.situationHint() == SituationHint.TRANSIENT) {
            feedbackLearningService.reverseEffects(feedbackId);
            classification.markApplied();
            classificationRepository.save(classification);
            return;
        }

        if (parsed.targetCorrection() != null || parsed.secondaryHint() != null) {
            feedbackLearningService.reapplyGardnerLearningWithOverride(
                    feedbackId, parsed.targetCorrection(), parsed.secondaryHint());
        }
        if (parsed.sensoryHint() != null) {
            feedbackLearningService.applySensoryHint(feedback.getChildId(), parsed.sensoryHint());
        }
        if (parsed.involvementHint() != null) {
            feedbackLearningService.applyInvolvementHint(feedback.getChildId(), parsed.involvementHint());
        }
        if (parsed.difficultyHint() != null) {
            feedbackLearningService.applyDifficultyHint(
                    feedback.getChildId(), feedback.getActivity().getTargetDomain(), parsed.difficultyHint());
        }
        // duration_hint: stored on the classification row only, no downstream effect yet
        // (Kidloop_FewShot_Prompt_v2.md §7 — out of scope, plan generation doesn't budget on it).

        classification.markApplied();
        classificationRepository.save(classification);
    }

    private boolean hasDataProcessingConsent(Long childId) {
        Child child = childRepository.findById(childId).orElse(null);
        if (child == null) return false;
        ParentProfile parentProfile = parentProfileRepository.findById(child.getParentId()).orElse(null);
        if (parentProfile == null) return false;
        return consentService.hasGrantedConsent(parentProfile.getUserId(), ConsentType.DATA_PROCESSING);
    }
}
