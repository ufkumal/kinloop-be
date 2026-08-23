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
import com.kinloop.backend.entity.Feedback;
import com.kinloop.backend.entity.FeedbackEffect;
import com.kinloop.backend.entity.enums.DunnQuadrant;
import com.kinloop.backend.entity.enums.FeedbackReason;
import com.kinloop.backend.entity.enums.FeedbackType;
import com.kinloop.backend.entity.enums.IntelligenceType;
import com.kinloop.backend.entity.enums.InvolvementType;
import com.kinloop.backend.exception.DailyPlanItemNotFoundException;
import com.kinloop.backend.exception.FeedbackAlreadySubmittedException;
import com.kinloop.backend.repository.ChildDomainLevelRepository;
import com.kinloop.backend.repository.ChildIntelligenceScoreRepository;
import com.kinloop.backend.repository.ChildProfileSnapshotRepository;
import com.kinloop.backend.repository.DailyPlanItemRepository;
import com.kinloop.backend.repository.DunnProfileRepository;
import com.kinloop.backend.repository.FeedbackEffectRepository;
import com.kinloop.backend.repository.FeedbackRepository;
import com.kinloop.backend.service.matching.MatchingParameters;
import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackLearningService {
    private final DailyPlanItemRepository dailyPlanItemRepository;
    private final FeedbackRepository feedbackRepository;
    private final FeedbackEffectRepository feedbackEffectRepository;
    private final ChildProfileSnapshotRepository profileRepository;
    private final DunnProfileRepository dunnRepository;
    private final ChildIntelligenceScoreRepository intelligenceRepository;
    private final ChildDomainLevelRepository domainRepository;
    private final MatchingParameters matchingParameters;

    @Transactional
    public ActivityFeedbackResponse submit(
            Child child,
            Long dailyPlanItemId,
            SubmitActivityFeedbackRequest request
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

        Map<IntelligenceType, ChildIntelligenceScore> scores = intelligenceRepository.findByChildId(child.getId())
                .stream().collect(Collectors.toMap(ChildIntelligenceScore::getIntelligenceType, Function.identity()));
        applyGardnerLearning(feedback, item.getActivity(), request.feedbackType(), reason, scores, parameters);

        ChildDomainLevel domainLevel = domainRepository.findByChildId(child.getId()).stream()
                .filter(value -> value.getDomain() == item.getActivity().getTargetDomain())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing domain level: " + item.getActivity().getTargetDomain()));
        applyDomainLearning(domainLevel, item.getActivity(), request.feedbackType(), parameters);
        item.complete();

        return new ActivityFeedbackResponse(
                feedback.getId(), item.getId(), request.feedbackType(), reason,
                domainLevel.getDomain(), domainLevel.getLevel(), domainLevel.getStreak());
    }

    private String normalizeFreeText(String freeText) {
        if (freeText == null) return null;
        String trimmed = freeText.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void applyGardnerLearning(
            Feedback feedback,
            Activity activity,
            FeedbackType type,
            FeedbackReason reason,
            Map<IntelligenceType, ChildIntelligenceScore> scores,
            Map<String, BigDecimal> parameters
    ) {
        ChildIntelligenceScore target = requiredScore(scores, activity.getTargetIntelligence());
        target.recordFeedbackSample();
        if (type == FeedbackType.LIKED) {
            applyDelta(feedback, target, parameters.get("liked_target_delta"), parameters);
            if (activity.getSecondaryIntelligence() != null) {
                applyDelta(feedback, requiredScore(scores, activity.getSecondaryIntelligence()),
                        parameters.get("liked_secondary_delta"), parameters);
            }
        } else if (type == FeedbackType.DISLIKED && reason == FeedbackReason.INTEREST) {
            applyDelta(feedback, target, parameters.get("disliked_interest_delta"), parameters);
        }
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
        domainLevel.applyFeedback(
                delta,
                parameters.get("level_up_threshold"),
                parameters.get("level_down_threshold"),
                parameters.get("level_min").shortValueExact(),
                parameters.get("level_max").shortValueExact(),
                parameters.get("ceiling_counter_cap"));
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
