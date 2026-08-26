package com.kinloop.backend.service;

import com.kinloop.backend.dto.feedback.ActivityFeedbackResponse;
import com.kinloop.backend.dto.feedback.SubmitActivityFeedbackRequest;
import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.ChildDomainLevel;
import com.kinloop.backend.entity.ChildIntelligenceScore;
import com.kinloop.backend.entity.ChildProfileSnapshot;
import com.kinloop.backend.entity.DailyPlanItem;
import com.kinloop.backend.entity.DunnProfile;
import com.kinloop.backend.entity.ChildSensoryAdjustment;
import com.kinloop.backend.entity.Feedback;
import com.kinloop.backend.entity.FeedbackEffect;
import com.kinloop.backend.entity.FeedbackLlmClassification;
import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.DifficultyHint;
import com.kinloop.backend.entity.enums.DunnQuadrant;
import com.kinloop.backend.entity.enums.FeedbackReason;
import com.kinloop.backend.entity.enums.FeedbackType;
import com.kinloop.backend.entity.enums.IntelligenceType;
import com.kinloop.backend.entity.enums.InvolvementHint;
import com.kinloop.backend.entity.enums.InvolvementType;
import com.kinloop.backend.entity.enums.SensoryHint;
import com.kinloop.backend.entity.enums.SituationHint;
import com.kinloop.backend.exception.DailyPlanItemNotFoundException;
import com.kinloop.backend.exception.FeedbackAlreadySubmittedException;
import com.kinloop.backend.repository.ChildDomainLevelRepository;
import com.kinloop.backend.repository.ChildIntelligenceScoreRepository;
import com.kinloop.backend.repository.ChildProfileSnapshotRepository;
import com.kinloop.backend.repository.ChildSensoryAdjustmentRepository;
import com.kinloop.backend.repository.DailyPlanItemRepository;
import com.kinloop.backend.repository.DunnProfileRepository;
import com.kinloop.backend.repository.FeedbackEffectRepository;
import com.kinloop.backend.repository.FeedbackLlmClassificationRepository;
import com.kinloop.backend.repository.FeedbackRepository;
import com.kinloop.backend.service.llm.FeedbackClassificationOutcome;
import com.kinloop.backend.service.llm.ParsedClassification;
import com.kinloop.backend.service.matching.MatchingParameters;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackLearningService {
    private final DailyPlanItemRepository dailyPlanItemRepository;
    private final FeedbackRepository feedbackRepository;
    private final FeedbackEffectRepository feedbackEffectRepository;
    private final ChildProfileSnapshotRepository profileRepository;
    private final DunnProfileRepository dunnRepository;
    private final ChildIntelligenceScoreRepository intelligenceRepository;
    private final ChildDomainLevelRepository domainRepository;
    private final MatchingParameters matchingParameters;
    private final ChildSensoryAdjustmentRepository sensoryAdjustmentRepository;
    private final FeedbackLlmClassificationRepository classificationRepository;

    @Transactional
    public ActivityFeedbackResponse submit(
            Child child,
            Long dailyPlanItemId,
            SubmitActivityFeedbackRequest request
    ) {
        return submit(child, dailyPlanItemId, request, FeedbackClassificationOutcome.notAttempted());
    }

    @Transactional
    public ActivityFeedbackResponse submit(
            Child child,
            Long dailyPlanItemId,
            SubmitActivityFeedbackRequest request,
            FeedbackClassificationOutcome classificationOutcome
    ) {
        DailyPlanItem item = dailyPlanItemRepository
                .findByIdAndDailyPlanChildId(dailyPlanItemId, child.getId())
                .orElseThrow(() -> new DailyPlanItemNotFoundException(dailyPlanItemId));
        if (feedbackRepository.existsByDailyPlanItemId(dailyPlanItemId)) {
            throw new FeedbackAlreadySubmittedException(dailyPlanItemId);
        }

        Map<String, BigDecimal> parameters = matchingParameters.load();
        ChildProfileSnapshot profile = profileRepository.findByChildIdAndCurrentTrue(child.getId())
                .orElseThrow(() -> new IllegalStateException("Child onboarding profile is missing"));
        FeedbackReason reason = resolveReason(request.feedbackType(), item.getActivity(), profile, parameters);
        String freeText = normalizeFreeText(request.freeText());
        Feedback feedback = feedbackRepository.save(
                new Feedback(child.getId(), item, request.feedbackType(), reason, freeText));

        ChildDomainLevel domainLevel = requiredDomainLevel(
                child.getId(), item.getActivity().getTargetDomain());
        ParsedClassification parsed = classificationOutcome.classification();
        if (parsed.valid()) {
            parsed = sanitizeClassification(parsed, item.getActivity(), request.feedbackType(), parameters);
        }
        BigDecimal confidenceThreshold = parameters.get("llm_feedback_confidence_threshold");
        boolean classificationApplicable = classificationOutcome.attempted()
                && parsed.valid()
                && confidenceThreshold != null
                && parsed.confidence().compareTo(confidenceThreshold) >= 0;
        if (classificationOutcome.attempted() && parsed.valid() && confidenceThreshold == null) {
            log.warn("LLM classification ignored because llm_feedback_confidence_threshold is missing");
        }
        boolean transientSituation = classificationApplicable
                && parsed.situationHint() == SituationHint.TRANSIENT;

        if (!transientSituation) {
            Map<IntelligenceType, ChildIntelligenceScore> scores = intelligenceRepository
                    .findByChildId(child.getId()).stream()
                    .collect(Collectors.toMap(ChildIntelligenceScore::getIntelligenceType, Function.identity()));
            applyGardnerLearning(
                    feedback,
                    item.getActivity(),
                    request.feedbackType(),
                    reason,
                    scores,
                    parameters,
                    classificationApplicable ? parsed.targetCorrection() : null,
                    classificationApplicable ? parsed.secondaryHint() : null);
            applyDomainLearning(domainLevel, item.getActivity(), request.feedbackType(), parameters);
        }

        if (classificationApplicable && !transientSituation) {
            applyIndependentHints(child.getId(), item.getActivity(), parsed);
        }
        if (classificationApplicable && parsed.conflict()) {
            log.warn("LLM_CONFLICT feedback={}", feedback.getId());
        }
        persistClassification(feedback, classificationOutcome, parsed, classificationApplicable);
        item.complete();

        return new ActivityFeedbackResponse(
                feedback.getId(), item.getId(), request.feedbackType(), reason,
                domainLevel.getDomain(), domainLevel.getLevel(), domainLevel.getStreak());
    }

    @Transactional
    public void reverseEffects(Long feedbackId) {
        var effects = feedbackEffectRepository.findByFeedbackIdAndReversedAtIsNull(feedbackId);
        if (effects.isEmpty()) return;

        Map<IntelligenceType, ChildIntelligenceScore> scores = intelligenceRepository
                .findByChildId(effects.getFirst().getFeedback().getChildId())
                .stream().collect(Collectors.toMap(
                        ChildIntelligenceScore::getIntelligenceType, Function.identity()));
        Map<String, BigDecimal> parameters = matchingParameters.load();
        for (FeedbackEffect effect : effects) {
            requiredScore(scores, effect.getIntelligenceType()).applyFeedback(
                    effect.getDelta().negate(),
                    parameters.get("gardner_runtime_min_score"),
                    parameters.get("gardner_runtime_max_score"));
            effect.reverse();
        }
    }

    private String normalizeFreeText(String freeText) {
        if (freeText == null) return null;
        String trimmed = freeText.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    void applyGardnerLearning(
            Feedback feedback,
            Activity activity,
            FeedbackType type,
            FeedbackReason reason,
            Map<IntelligenceType, ChildIntelligenceScore> scores,
            Map<String, BigDecimal> parameters,
            IntelligenceType targetCorrection,
            IntelligenceType secondaryHint
    ) {
        IntelligenceType targetType = activity.getTargetIntelligence();
        IntelligenceType secondaryType = activity.getSecondaryIntelligence();
        ChildIntelligenceScore target = requiredScore(scores, targetType);
        target.recordFeedbackSample();
        if (type == FeedbackType.LIKED) {
            if (targetCorrection != targetType) {
                applyDelta(feedback, target, parameters.get("liked_target_delta"), parameters);
            }
            if (secondaryType != null && secondaryType != targetType) {
                applyDelta(feedback, requiredScore(scores, secondaryType),
                        parameters.get("liked_secondary_delta"), parameters);
            }
            if (secondaryHint != null
                    && secondaryHint != targetType
                    && secondaryHint != secondaryType) {
                applyDelta(feedback, requiredScore(scores, secondaryHint),
                        parameters.get("liked_secondary_delta"), parameters);
            }
        } else if (type == FeedbackType.DISLIKED && reason == FeedbackReason.INTEREST) {
            applyDelta(feedback, target, parameters.get("disliked_interest_delta"), parameters);
        }
    }

    private ParsedClassification sanitizeClassification(
            ParsedClassification parsed,
            Activity activity,
            FeedbackType feedbackType,
            Map<String, BigDecimal> parameters
    ) {
        IntelligenceType targetCorrection = parsed.targetCorrection();
        IntelligenceType secondaryHint = parsed.secondaryHint();
        boolean conflict = parsed.conflict();

        if (feedbackType == FeedbackType.DISLIKED && targetCorrection != null) {
            targetCorrection = null;
            conflict = false;
        } else if (targetCorrection != null
                && targetCorrection != activity.getTargetIntelligence()) {
            targetCorrection = null;
        } else if (feedbackType == FeedbackType.LIKED && targetCorrection != null) {
            conflict = true;
        }

        if (secondaryHint == activity.getTargetIntelligence()
                || secondaryHint == activity.getSecondaryIntelligence()
                || secondaryHint == targetCorrection) {
            secondaryHint = null;
        }

        BigDecimal maximum = parameters.get("llm_max_affected_domains");
        if (secondaryHint != null) {
            if (maximum == null) {
                log.warn("LLM secondary_hint ignored because llm_max_affected_domains is missing");
                secondaryHint = null;
            } else {
                Set<IntelligenceType> affected = new HashSet<>();
                affected.add(activity.getTargetIntelligence());
                if (activity.getSecondaryIntelligence() != null) {
                    affected.add(activity.getSecondaryIntelligence());
                }
                affected.add(secondaryHint);
                if (affected.size() > maximum.intValue()) {
                    secondaryHint = null;
                }
            }
        }

        return parsed.withGardnerHints(targetCorrection, secondaryHint, conflict);
    }

    private void applyDelta(
            Feedback feedback,
            ChildIntelligenceScore score,
            BigDecimal requestedDelta,
            Map<String, BigDecimal> parameters
    ) {
        BigDecimal appliedDelta = score.applyFeedback(
                requestedDelta,
                parameters.get("gardner_runtime_min_score"),
                parameters.get("gardner_runtime_max_score"));
        if (appliedDelta.signum() != 0) {
            feedbackEffectRepository.save(new FeedbackEffect(
                    feedback, score.getIntelligenceType(), appliedDelta));
        }
    }

    private void applyDomainLearning(
            ChildDomainLevel domainLevel,
            Activity activity,
            FeedbackType type,
            Map<String, BigDecimal> parameters
    ) {
        BigDecimal delta = domainDelta(domainLevel.getLevel(), activity.getDifficulty(), type, parameters);
        if (delta.signum() == 0) return;
        applyDomainDelta(domainLevel, delta, parameters);
    }

    @Transactional
    public void applyDifficultyHint(
            Long childId,
            DevelopmentDomain domain,
            DifficultyHint hint
    ) {
        Map<String, BigDecimal> parameters = matchingParameters.load();
        BigDecimal delta = switch (hint) {
            case HARDER -> parameters.get("llm_difficulty_hint_harder_delta");
            case EASIER -> parameters.get("llm_difficulty_hint_easier_delta");
        };
        applyDomainDelta(requiredDomainLevel(childId, domain), delta, parameters);
    }

    @Transactional
    public void applySensoryHint(Long childId, SensoryHint hint) {
        ChildSensoryAdjustment adjustment = requiredSensoryAdjustment(childId);
        short step = matchingParameters.load().get("llm_sensory_tolerance_step").shortValueExact();
        adjustment.applySensoryAdjustment(hint, step);
        sensoryAdjustmentRepository.save(adjustment);
    }

    @Transactional
    public void applyInvolvementHint(Long childId, InvolvementHint hint) {
        ChildSensoryAdjustment adjustment = requiredSensoryAdjustment(childId);
        adjustment.applyInvolvementFilter(hint);
        sensoryAdjustmentRepository.save(adjustment);
    }

    private ChildSensoryAdjustment requiredSensoryAdjustment(Long childId) {
        return sensoryAdjustmentRepository.findByChildId(childId)
                .orElseGet(() -> new ChildSensoryAdjustment(childId, (short) 0, (short) 0, (short) 0, null));
    }

    private void applyIndependentHints(Long childId, Activity activity, ParsedClassification parsed) {
        if (parsed.sensoryHint() != null) {
            applySensoryHint(childId, parsed.sensoryHint());
        }
        if (parsed.involvementHint() != null) {
            applyInvolvementHint(childId, parsed.involvementHint());
        }
        if (parsed.difficultyHint() != null) {
            applyDifficultyHint(childId, activity.getTargetDomain(), parsed.difficultyHint());
        }
        // durationHint is intentionally persistence-only in v2.
    }

    private void persistClassification(
            Feedback feedback,
            FeedbackClassificationOutcome outcome,
            ParsedClassification parsed,
            boolean applied
    ) {
        if (!outcome.attempted()) return;
        FeedbackLlmClassification classification = new FeedbackLlmClassification(
                feedback,
                outcome.modelName(),
                outcome.rawResponse(),
                parsed.confidence(),
                parsed.targetCorrection(),
                parsed.secondaryHint(),
                parsed.sensoryHint(),
                parsed.involvementHint(),
                parsed.difficultyHint(),
                parsed.situationHint(),
                parsed.durationHint(),
                parsed.conflict());
        if (applied) classification.markApplied();
        classificationRepository.save(classification);
    }

    private void applyDomainDelta(
            ChildDomainLevel domainLevel,
            BigDecimal delta,
            Map<String, BigDecimal> parameters
    ) {
        domainLevel.applyFeedback(
                delta,
                parameters.get("level_up_threshold"),
                parameters.get("level_down_threshold"),
                parameters.get("level_min").shortValueExact(),
                parameters.get("level_max").shortValueExact(),
                parameters.get("ceiling_counter_cap"));
    }

    private ChildDomainLevel requiredDomainLevel(Long childId, DevelopmentDomain domain) {
        return domainRepository.findByChildId(childId).stream()
                .filter(value -> value.getDomain() == domain)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing domain level: " + domain));
    }

    private BigDecimal domainDelta(
            short level,
            short difficulty,
            FeedbackType type,
            Map<String, BigDecimal> parameters
    ) {
        if (type == FeedbackType.LIKED) {
            if (difficulty >= level + 1) return parameters.get("level_credit_stretch");
            if (difficulty == level) return parameters.get("level_credit_at_level");
            return parameters.get("level_credit_below");
        }
        if (type == FeedbackType.STRUGGLED) {
            if (difficulty > level + 1) return BigDecimal.ZERO;
            if (difficulty == level + 1) return parameters.get("level_penalty_struggle_stretch");
            return parameters.get("level_penalty_struggle_at_level");
        }
        return BigDecimal.ZERO;
    }

    private FeedbackReason resolveReason(
            FeedbackType type,
            Activity activity,
            ChildProfileSnapshot profile,
            Map<String, BigDecimal> parameters
    ) {
        if (type != FeedbackType.DISLIKED) return null;
        DunnQuadrant quadrant = profile.getDunnQuadrant() == null ? DunnQuadrant.MIXED : profile.getDunnQuadrant();
        if (quadrant == DunnQuadrant.C3 || quadrant == DunnQuadrant.C4) {
            DunnProfile dunn = dunnRepository.findById(quadrant)
                    .orElseThrow(() -> new IllegalStateException("Dunn profile is missing: " + quadrant));
            if (activity.getNoiseLoad() > dunn.getNoiseTolerance()
                    || activity.getVisualLoad() > dunn.getVisualTolerance()
                    || activity.getPhysicalIntensity() > dunn.getMovementTolerance()) {
                return FeedbackReason.SENSORY;
            }
        }
        boolean anxious = profile.getSeparationAnxiety() != null
                && profile.getSeparationAnxiety() >= parameters.get("attachment_anxiety_threshold").intValueExact();
        if (anxious && activity.getInvolvementType() == InvolvementType.BAGIMSIZ) {
            return FeedbackReason.INVOLVEMENT;
        }
        return FeedbackReason.INTEREST;
    }

    private ChildIntelligenceScore requiredScore(
            Map<IntelligenceType, ChildIntelligenceScore> scores,
            IntelligenceType type
    ) {
        ChildIntelligenceScore score = scores.get(type);
        if (score == null) throw new IllegalStateException("Missing intelligence score: " + type);
        return score;
    }
}
