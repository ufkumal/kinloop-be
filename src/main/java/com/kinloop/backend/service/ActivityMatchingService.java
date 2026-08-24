package com.kinloop.backend.service;

import com.kinloop.backend.dto.matching.*;
import com.kinloop.backend.entity.*;
import com.kinloop.backend.entity.enums.*;
import com.kinloop.backend.exception.DailyPlanNotFoundException;
import com.kinloop.backend.repository.*;
import com.kinloop.backend.service.matching.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates one use case; persistence and business policies live behind collaborators.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityMatchingService {
    private final ChildRepository childRepository;
    private final ChildProfileSnapshotRepository profileRepository;
    private final ChildSensoryAdjustmentRepository sensoryAdjustmentRepository;
    private final ActivityRepository activityRepository;
    private final DunnProfileRepository dunnRepository;
    private final DevelopmentalPeriodTaskRepository periodRepository;
    private final ChildIntelligenceScoreRepository intelligenceRepository;
    private final ChildDomainLevelRepository domainRepository;
    private final DailyPlanRepository planRepository;
    private final RecommendationRepository recommendationRepository;
    private final MatchingParameters parameters;
    private final MatchingStateInitializer stateInitializer;
    private final ActivityEligibilityPolicy eligibilityPolicy;
    private final ActivityFreshnessPolicy freshnessPolicy;
    private final ActivityScorer scorer;
    private final CandidateOrdering candidateOrdering;
    private final DailyPortfolioBuilder portfolioBuilder;
    private final OnboardingService onboardingService;
    private final ParentProfileRepository parentProfileRepository;
    private final ConsentService consentService;

    @Transactional
    public DailyPlanResponse today(Child requestedChild) {
        Child child = childRepository.findLockedById(requestedChild.getId()).orElseThrow();
        LocalDate today = LocalDate.now();
        Optional<DailyPlan> existing = planRepository.findByChildIdAndPlanDate(child.getId(), today);
        if (existing.isPresent()) return response(existing.get(), child);

        Long userId = parentProfileRepository.findById(child.getParentId())
                .filter(parent -> parent.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalStateException("Parent profile not found"))
                .getUserId();
        consentService.requireAllRequiredConsents(userId);

        ChildProfileSnapshot profile = profileRepository.findByChildIdAndCurrentTrue(child.getId())
                .orElseThrow(() -> new IllegalStateException("Child onboarding profile is missing"));
        short budget = child.getDailyTimeBudgetMax();
        int ageMonths = child.ageInMonths(today);
        Map<String, BigDecimal> p = parameters.load();
        stateInitializer.initialize(child.getId(), ageMonths, profile.getGardnerPriors(), p);

        DunnQuadrant quadrant = profile.getDunnQuadrant() == null ? DunnQuadrant.MIXED : profile.getDunnQuadrant();
        DunnProfile dunn = dunnRepository.findById(quadrant).orElseThrow(() -> new IllegalStateException("Dunn profile is missing: " + quadrant));
        ChildSensoryAdjustment sensoryAdjustment = sensoryAdjustmentRepository.findByChildId(child.getId())
                .orElse(null);
        DevelopmentDomain period = periodRepository.findForAge(ageMonths).orElseThrow(() -> new IllegalStateException("Developmental period is missing")).getTargetDomain();
        Map<IntelligenceType, ChildIntelligenceScore> scores = intelligenceRepository.findByChildId(child.getId()).stream().collect(Collectors.toMap(ChildIntelligenceScore::getIntelligenceType, Function.identity()));
        Map<DevelopmentDomain, ChildDomainLevel> levels = domainRepository.findByChildId(child.getId()).stream().collect(Collectors.toMap(ChildDomainLevel::getDomain, Function.identity()));
        List<Activity> preFreshnessPool = activityRepository.findEligibleBasePool(ageMonths, budget).stream()
                .filter(a -> eligibilityPolicy.allows(a, profile, sensoryAdjustment, p))
                .toList();
        int freshnessWindow = freshnessPolicy.windowSize(preFreshnessPool.size(), p);
        Set<Long> recentActivityIds = planRepository.findActivityIdsInRecentPlans(
                child.getId(), today, freshnessWindow);
        ActivityFreshnessPolicy.Result activityPool = freshnessPolicy.eliminate(
                preFreshnessPool, recentActivityIds, freshnessWindow);

        List<ScoredActivity> preFreshnessScored = activityPool.preFreshnessPool().stream()
                .map(a -> scorer.score(a, profile, dunn, sensoryAdjustment, period, scores, levels, p))
                .sorted(candidateOrdering.comparator(child.getId(), today, scores, p))
                .toList();
        Set<Long> freshIds = activityPool.eligiblePool().stream().map(Activity::getId).collect(Collectors.toSet());
        List<ScoredActivity> freshScored = preFreshnessScored.stream()
                .filter(candidate -> freshIds.contains(candidate.activity().getId()))
                .toList();
        boolean supervisedGuarantee = profile.getSeparationAnxiety() != null
                && profile.getSeparationAnxiety() >= p.get("attachment_anxiety_threshold").intValueExact()
                && p.get("attachment_guarantee_supervised").signum() != 0;
        DailyPortfolioBuilder.Result portfolio = portfolioBuilder.build(new DailyPortfolioBuilder.Request(
                freshScored,
                preFreshnessScored,
                activityPool.excludedActivityIds(),
                period,
                scores,
                budget,
                supervisedGuarantee
        ));
        portfolio.warnings().forEach(warning -> log.warn(
                "daily_plan_fallback childId={} profile={} missingSlot={} fallbackLevel={} reason={}",
                child.getId(), quadrant, warning.missingSlot(), warning.fallbackLevel(), warning.reason()));

        DailyPlan plan = new DailyPlan(
                child.getId(), today, child.getDailyTimeBudgetMin(), child.getDailyTimeBudgetMax());
        plan.recordBookkeeping(
                portfolio.committedDurationMinutes(),
                portfolio.totalDurationMinutes(),
                portfolio.fallbackLevel());
        short rank = 1;
        for (var selected : portfolio.selections()) {
            ScoredActivity match = selected.activity();
            recommendationRepository.save(new Recommendation(child.getId(), match.activity(), match.rawScore(), rank++, match.breakdown()));
            plan.add(match.activity(), selected.slot(), match.rawScore(),
                    selected.withinBudget(), selected.repeatNotice());
        }
        return response(planRepository.save(plan), child);
    }

    @Transactional
    public DailyPlanResponse selectActivity(Child child, Long activityId) {
        DailyPlan plan = planRepository.findByChildIdAndPlanDate(child.getId(), LocalDate.now())
                .orElseThrow(() -> new DailyPlanNotFoundException(child.getId()));
        plan.prepareSelection(activityId);
        planRepository.saveAndFlush(plan);
        plan.select(activityId);
        return response(planRepository.save(plan), child);
    }

    private DailyPlanResponse response(DailyPlan plan, Child child) {
        List<DailyActivityResponse> items = plan.getItems().stream()
                .distinct()
                .sorted(Comparator.comparingInt(x -> x.getSlotType().ordinal()))
                .map(this::response)
                .toList();
        return new DailyPlanResponse(
                plan.getId(),
                plan.getChildId(),
                plan.getPlanDate(),
                plan.getBudgetMin(),
                plan.getBudgetMax(),
                plan.getCommittedDurationMinutes(),
                plan.getTotalDurationMinutes(),
                plan.getFallbackLevel(),
                items,
                plan.getFallbackLevel() == 4 ? "EMPTY_POOL" : "READY",
                plan.getFallbackLevel() == 4
                        ? "Bugün çocuğunuza uygun bir etkinlik planı oluşturamadık. Lütfen daha sonra tekrar deneyin."
                        : null,
                onboardingService.shouldShowPlanReminder(
                        child, planRepository.countByChildId(child.getId()))
        );
    }

    private DailyActivityResponse response(DailyPlanItem item) {
        Activity a = item.getActivity();
        ActivityInstruction i = a.getInstruction();
        List<ActivityStepResponse> steps = a.getSteps().stream()
                .sorted(Comparator.comparingInt(ActivityStep::getStepNo))
                .map(step -> new ActivityStepResponse(step.getStepNo(), step.getText()))
                .toList();
        List<ActivityMaterialResponse> materials = a.getMaterials().stream()
                .sorted(Comparator.comparingInt(ActivityMaterial::getDisplayOrder)
                        .thenComparing(ActivityMaterial::getId))
                .map(material -> new ActivityMaterialResponse(
                        material.getName(), material.getCategory(), material.getQuantity(),
                        material.isOptional(), material.getDisplayOrder(), material.getNote()))
                .toList();
        List<ActivityOutcomeResponse> outcomes = a.getOutcomes().stream()
                .sorted(Comparator.comparingInt(ActivityOutcome::getDisplayOrder)
                        .thenComparing(ActivityOutcome::getId))
                .map(outcome -> new ActivityOutcomeResponse(
                        outcome.getDisplayOrder(), outcome.getOutcome()))
                .toList();
        return new DailyActivityResponse(
                item.getId(), a.getId(), a.getTitle(), a.getDescription(),
                a.getMinAgeMonths(), a.getMaxAgeMonths(), a.getTargetIntelligence(),
                a.getSecondaryIntelligence(), a.getTargetDomain(), a.getDifficulty(),
                a.getDurationMinutes(), a.getInvolvementType(), a.getNoiseLoad(),
                a.getVisualLoad(), a.getPhysicalIntensity(), item.getSlotType().name(), item.getScore(),
                i == null ? null : i.getIntro(), i == null ? null : i.getPurpose(),
                i == null ? null : i.getWhyItMatters(), i == null ? null : i.getEasierVariation(),
                i == null ? null : i.getHarderVariation(), i == null ? null : i.getObservationTip(),
                i == null ? null : i.getSafetyNotes(), i == null ? null : i.getCleanupNotes(),
                steps, materials, outcomes, item.isWithinBudget(), item.isRepeatNotice(),
                item.isSelected(), item.isCompleted(), item.getSelectedAt(), item.getCompletedAt());
    }
}
