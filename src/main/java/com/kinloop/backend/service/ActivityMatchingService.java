package com.kinloop.backend.service;

import com.kinloop.backend.dto.matching.*;
import com.kinloop.backend.entity.*;
import com.kinloop.backend.entity.enums.*;
import com.kinloop.backend.exception.MissingDailyTimeBudgetException;
import com.kinloop.backend.repository.*;
import com.kinloop.backend.service.matching.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates one use case; persistence and business policies live behind collaborators.
 */
@Service
@RequiredArgsConstructor
public class ActivityMatchingService {
    private final ChildRepository childRepository;
    private final ParentProfileRepository parentRepository;
    private final ChildProfileSnapshotRepository profileRepository;
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
    private final ActivityScorer scorer;
    private final DailyPortfolioBuilder portfolioBuilder;
    private final StrengthenCandidateSelector strengthenCandidateSelector;

    @Transactional
    public DailyPlanResponse today(Child requestedChild) {
        Child child = childRepository.findLockedById(requestedChild.getId()).orElseThrow();
        LocalDate today = LocalDate.now();
        Optional<DailyPlan> existing = planRepository.findByChildIdAndPlanDate(child.getId(), today);
        if (existing.isPresent()) return response(existing.get(), budget(child));

        ChildProfileSnapshot profile = profileRepository.findByChildIdAndCurrentTrue(child.getId())
                .orElseThrow(() -> new IllegalStateException("Child onboarding profile is missing"));
        short budget = budget(child);
        Map<String, BigDecimal> p = parameters.load();
        stateInitializer.initialize(child.getId(), profile.getGardnerPriors(), p);

        DunnQuadrant quadrant = profile.getDunnQuadrant() == null ? DunnQuadrant.MIXED : profile.getDunnQuadrant();
        DunnProfile dunn = dunnRepository.findById(quadrant).orElseThrow(() -> new IllegalStateException("Dunn profile is missing: " + quadrant));
        DevelopmentDomain period = periodRepository.findForAge(child.ageInMonths(today)).orElseThrow(() -> new IllegalStateException("Developmental period is missing")).getTargetDomain();
        Map<IntelligenceType, ChildIntelligenceScore> scores = intelligenceRepository.findByChildId(child.getId()).stream().collect(Collectors.toMap(ChildIntelligenceScore::getIntelligenceType, Function.identity()));
        Map<DevelopmentDomain, ChildDomainLevel> levels = domainRepository.findByChildId(child.getId()).stream().collect(Collectors.toMap(ChildDomainLevel::getDomain, Function.identity()));
        Set<Long> yesterday = planRepository.findActivityIds(child.getId(), today.minusDays(p.get("freshness_lookback_days").longValue()));

        List<ScoredActivity> scored = activityRepository.findEligibleBasePool(child.ageInMonths(today), budget).stream()
                .filter(a -> eligibilityPolicy.allows(a, profile, p))
                .map(a -> scorer.score(a, profile, dunn, period, scores, levels, yesterday, p))
                .sorted(Comparator.comparing(ScoredActivity::rawScore).reversed().thenComparing(a -> a.activity().getId())).toList();
        if (scored.isEmpty()) throw new IllegalStateException("No eligible activities for this child today");

        int leastSamples = scores.values().stream().mapToInt(ChildIntelligenceScore::getFeedbackCount).min().orElse(0);
        Set<IntelligenceType> leastKnown = scores.values().stream().filter(x -> x.getFeedbackCount() == leastSamples).map(ChildIntelligenceScore::getIntelligenceType).collect(Collectors.toSet());
        int limit = p.get("slot_candidate_limit").intValue();
        int exploreLimit = p.get("explore_random_top_limit").intValue();
        List<ScoredActivity> develop = top(scored.stream().filter(x -> x.activity().getTargetDomain() == period).toList(), limit);
        List<ScoredActivity> strengthen = strengthenCandidateSelector.select(scored, scores, limit);
        List<ScoredActivity> explore = top(scored.stream().filter(x -> leastKnown.contains(x.activity().getTargetIntelligence())).toList(), exploreLimit);
        // TODO(v5-policy): Tue/Thu/Sat second-strengthening behavior awaits a distinct persistence slot (TBA item 7).
        List<DailyPortfolioBuilder.Selection> selection = portfolioBuilder.build(develop, strengthen, explore, budget);
        if (selection.isEmpty()) throw new IllegalStateException("No development activity fits the daily budget");

        DailyPlan plan = new DailyPlan(child.getId(), today);
        short rank = 1;
        for (var selected : selection) {
            ScoredActivity match = selected.activity();
            recommendationRepository.save(new Recommendation(child.getId(), match.activity(), match.rawScore(), rank++, match.breakdown()));
            plan.add(match.activity(), selected.slot(), match.rawScore());
        }
        return response(planRepository.save(plan), budget);
    }

    private short budget(Child child) {
        return parentRepository.findById(child.getParentId())
                .filter(parent -> parent.getDeletedAt() == null)
                .map(ParentProfile::getDailyTimeBudgetMinutes)
                .orElseThrow(MissingDailyTimeBudgetException::new);
    }

    private List<ScoredActivity> top(List<ScoredActivity> values, int limit) {
        return values.stream().limit(limit).toList();
    }

    private DailyPlanResponse response(DailyPlan plan, short budget) {
        List<DailyActivityResponse> items = plan.getItems().stream().sorted(Comparator.comparingInt(x -> x.getSlotType().ordinal())).map(this::response).toList();
        return new DailyPlanResponse(plan.getId(), plan.getChildId(), plan.getPlanDate(), budget, items.stream().mapToInt(DailyActivityResponse::durationMinutes).sum(), items);
    }

    private DailyActivityResponse response(DailyPlanItem item) {
        Activity a = item.getActivity();
        ActivityInstruction i = a.getInstruction();
        return new DailyActivityResponse(a.getId(), a.getTitle(), a.getDescription(), a.getDurationMinutes(), item.getSlotType().name(), item.getScore(),
                i == null ? null : i.getIntro(), i == null ? null : i.getPurpose(), i == null ? null : i.getWhyItMatters(), i == null ? null : i.getEasierVariation(), i == null ? null : i.getHarderVariation(), i == null ? null : i.getObservationTip());
    }
}
