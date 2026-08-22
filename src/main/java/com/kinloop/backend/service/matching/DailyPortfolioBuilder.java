package com.kinloop.backend.service.matching;

import com.kinloop.backend.entity.ChildIntelligenceScore;
import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.IntelligenceType;
import com.kinloop.backend.entity.enums.InvolvementType;
import com.kinloop.backend.entity.enums.PlanSlotType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DailyPortfolioBuilder {

    public Result build(Request request) {
        List<Warning> warnings = new ArrayList<>();

        Attempt attempt = fill(request.freshPool(), request, false, false, false);
        if (attempt.complete()) return result(attempt, request, (short) 0, warnings, request.freshPool());

        warnings.add(warning((short) 1, attempt.missingSlot(), "freshness_elimination_removed_required_candidates"));
        attempt = fill(request.preFreshnessPool(), request, false, false, true);
        if (attempt.complete()) return result(attempt, request, (short) 1, warnings, request.preFreshnessPool());

        warnings.add(warning((short) 2, attempt.missingSlot(), "strict_slot_constraint_unfilled"));
        attempt = fill(request.preFreshnessPool(), request, true, false, true);
        if (attempt.complete()) return result(attempt, request, (short) 2, warnings, request.preFreshnessPool());

        warnings.add(warning((short) 3, attempt.missingSlot(), "complete_three_slot_plan_unavailable"));
        attempt = fill(request.preFreshnessPool(), request, true, true, true);
        if (!attempt.selections().isEmpty()) {
            return result(attempt, request, (short) 3, warnings, request.preFreshnessPool());
        }

        warnings.add(warning((short) 4, PlanSlotType.DEVELOP, "eligible_activity_pool_empty"));
        return new Result(List.of(), (short) 4, 0, 0, List.copyOf(warnings));
    }

    private Result result(
            Attempt attempt,
            Request request,
            short fallbackLevel,
            List<Warning> warnings,
            List<ScoredActivity> sourcePool
    ) {
        List<Selection> selections = applySupervisedGuarantee(
                attempt.selections(), sourcePool, request);
        int committedDuration = selections.stream()
                .filter(Selection::withinBudget)
                .mapToInt(selection -> selection.activity().activity().getDurationMinutes())
                .sum();
        int totalDuration = selections.stream()
                .mapToInt(selection -> selection.activity().activity().getDurationMinutes())
                .sum();
        return new Result(List.copyOf(selections), fallbackLevel, committedDuration, totalDuration,
                List.copyOf(warnings));
    }

    private Attempt fill(
            List<ScoredActivity> pool,
            Request request,
            boolean relaxSlots,
            boolean allowPartial,
            boolean freshnessRelaxed
    ) {
        if (pool.isEmpty()) return new Attempt(List.of(), PlanSlotType.DEVELOP);

        List<Selection> selections = new ArrayList<>();
        Set<Long> selectedIds = new LinkedHashSet<>();
        int remaining = request.budgetMax();
        int reserve = pool.stream()
                .mapToInt(candidate -> candidate.activity().getDurationMinutes())
                .min().orElse(0);

        ScoredActivity development = firstFitting(
                relaxSlots ? pool : byDomain(pool, request.periodDomain()),
                selectedIds,
                Math.max(0, remaining - reserve));
        if (development == null && allowPartial) {
            development = firstFitting(pool, selectedIds, remaining);
        }
        if (development == null && !allowPartial) return new Attempt(selections, PlanSlotType.DEVELOP);
        if (development != null) {
            add(selections, selectedIds, development, PlanSlotType.DEVELOP, true,
                    repeatNotice(development, request, freshnessRelaxed));
            remaining -= development.activity().getDurationMinutes();
        }

        ScoredActivity strengthening = relaxSlots
                ? firstFitting(pool, selectedIds, remaining)
                : strongestFitting(pool, selectedIds, remaining, request.intelligenceScores());
        if (strengthening == null && !allowPartial) return new Attempt(selections, PlanSlotType.STRENGTHEN);
        if (strengthening != null) {
            add(selections, selectedIds, strengthening, PlanSlotType.STRENGTHEN, true,
                    repeatNotice(strengthening, request, freshnessRelaxed));
            remaining -= strengthening.activity().getDurationMinutes();
        }

        List<ScoredActivity> explorationCandidates = relaxSlots
                ? available(pool, selectedIds)
                : leastSampledCandidates(pool, selectedIds, request.intelligenceScores());
        ScoredActivity exploration = firstFitting(explorationCandidates, selectedIds, remaining);
        boolean explorationWithinBudget = true;
        if (exploration == null && !explorationCandidates.isEmpty()) {
            exploration = shortest(explorationCandidates);
            explorationWithinBudget = false;
        }
        if (exploration == null && !allowPartial) return new Attempt(selections, PlanSlotType.EXPLORE);
        if (exploration != null) {
            add(selections, selectedIds, exploration, PlanSlotType.EXPLORE, explorationWithinBudget,
                    repeatNotice(exploration, request, freshnessRelaxed));
        }

        return new Attempt(selections, selections.size() == 3 ? null : nextMissingSlot(selections));
    }

    private List<Selection> applySupervisedGuarantee(
            List<Selection> current,
            List<ScoredActivity> pool,
            Request request
    ) {
        if (!request.supervisedGuarantee()
                || current.size() != 3
                || current.stream().anyMatch(this::isSupervised)) {
            return current;
        }

        Set<Long> protectedIds = current.stream()
                .filter(selection -> selection.slot() != PlanSlotType.EXPLORE)
                .map(selection -> selection.activity().activity().getId())
                .collect(java.util.stream.Collectors.toSet());
        List<ScoredActivity> supervised = pool.stream()
                .filter(candidate -> candidate.activity().getInvolvementType() == InvolvementType.GOZETIMLI)
                .filter(candidate -> !protectedIds.contains(candidate.activity().getId()))
                .toList();
        if (supervised.isEmpty()) return current;

        int committedBeforeExploration = current.stream()
                .filter(selection -> selection.slot() != PlanSlotType.EXPLORE)
                .filter(Selection::withinBudget)
                .mapToInt(selection -> selection.activity().activity().getDurationMinutes())
                .sum();
        int remaining = request.budgetMax() - committedBeforeExploration;
        ScoredActivity replacement = firstFitting(supervised, protectedIds, remaining);
        boolean withinBudget = replacement != null;
        if (replacement == null) replacement = shortest(supervised);

        List<Selection> replaced = new ArrayList<>(current);
        int explorationIndex = java.util.stream.IntStream.range(0, replaced.size())
                .filter(index -> replaced.get(index).slot() == PlanSlotType.EXPLORE)
                .findFirst().orElseThrow();
        replaced.set(explorationIndex, new Selection(
                PlanSlotType.EXPLORE,
                replacement,
                withinBudget,
                request.recentActivityIds().contains(replacement.activity().getId())
        ));
        return replaced;
    }

    private ScoredActivity strongestFitting(
            List<ScoredActivity> pool,
            Set<Long> selectedIds,
            int remaining,
            Map<IntelligenceType, ChildIntelligenceScore> scores
    ) {
        List<BigDecimal> sortedScores = scores.values().stream()
                .map(ChildIntelligenceScore::getScore)
                .sorted(Comparator.reverseOrder())
                .toList();
        List<BigDecimal> scoreBands = new ArrayList<>();
        for (BigDecimal score : sortedScores) {
            if (scoreBands.isEmpty() || scoreBands.getLast().compareTo(score) != 0) scoreBands.add(score);
        }
        if (scoreBands.size() <= 1) return firstFitting(pool, selectedIds, remaining);

        for (BigDecimal band : scoreBands.stream().limit(3).toList()) {
            EnumSet<IntelligenceType> types = scores.values().stream()
                    .filter(score -> score.getScore().compareTo(band) == 0)
                    .map(ChildIntelligenceScore::getIntelligenceType)
                    .collect(() -> EnumSet.noneOf(IntelligenceType.class), EnumSet::add, EnumSet::addAll);
            ScoredActivity candidate = firstFitting(pool.stream()
                    .filter(value -> types.contains(value.activity().getTargetIntelligence())).toList(),
                    selectedIds, remaining);
            if (candidate != null) return candidate;
        }
        return null;
    }

    private List<ScoredActivity> leastSampledCandidates(
            List<ScoredActivity> pool,
            Set<Long> selectedIds,
            Map<IntelligenceType, ChildIntelligenceScore> scores
    ) {
        int minimum = scores.values().stream()
                .mapToInt(ChildIntelligenceScore::getFeedbackCount)
                .min().orElse(0);
        EnumSet<IntelligenceType> leastSampled = scores.values().stream()
                .filter(score -> score.getFeedbackCount() == minimum)
                .map(ChildIntelligenceScore::getIntelligenceType)
                .collect(() -> EnumSet.noneOf(IntelligenceType.class), EnumSet::add, EnumSet::addAll);
        List<ScoredActivity> candidates = available(pool, selectedIds).stream()
                .filter(value -> leastSampled.contains(value.activity().getTargetIntelligence()))
                .toList();
        return candidates.isEmpty() ? available(pool, selectedIds) : candidates;
    }

    private List<ScoredActivity> byDomain(List<ScoredActivity> pool, DevelopmentDomain domain) {
        return pool.stream().filter(candidate -> candidate.activity().getTargetDomain() == domain).toList();
    }

    private List<ScoredActivity> available(List<ScoredActivity> pool, Set<Long> selectedIds) {
        return pool.stream().filter(candidate -> !selectedIds.contains(candidate.activity().getId())).toList();
    }

    private ScoredActivity firstFitting(List<ScoredActivity> candidates, Set<Long> selectedIds, int maximumDuration) {
        return candidates.stream()
                .filter(candidate -> !selectedIds.contains(candidate.activity().getId()))
                .filter(candidate -> candidate.activity().getDurationMinutes() <= maximumDuration)
                .findFirst().orElse(null);
    }

    private ScoredActivity shortest(List<ScoredActivity> candidates) {
        int minimumDuration = candidates.stream()
                .mapToInt(candidate -> candidate.activity().getDurationMinutes())
                .min().orElseThrow();
        return candidates.stream()
                .filter(candidate -> candidate.activity().getDurationMinutes() == minimumDuration)
                .findFirst().orElseThrow();
    }

    private void add(
            List<Selection> selections,
            Set<Long> selectedIds,
            ScoredActivity activity,
            PlanSlotType slot,
            boolean withinBudget,
            boolean repeatNotice
    ) {
        selections.add(new Selection(slot, activity, withinBudget, repeatNotice));
        selectedIds.add(activity.activity().getId());
    }

    private boolean repeatNotice(ScoredActivity activity, Request request, boolean freshnessRelaxed) {
        return freshnessRelaxed && request.recentActivityIds().contains(activity.activity().getId());
    }

    private boolean isSupervised(Selection selection) {
        return selection.activity().activity().getInvolvementType() == InvolvementType.GOZETIMLI;
    }

    private PlanSlotType nextMissingSlot(List<Selection> selections) {
        Set<PlanSlotType> present = selections.stream().map(Selection::slot)
                .collect(java.util.stream.Collectors.toSet());
        return java.util.stream.Stream.of(
                        PlanSlotType.DEVELOP, PlanSlotType.STRENGTHEN, PlanSlotType.EXPLORE)
                .filter(slot -> !present.contains(slot))
                .findFirst().orElse(PlanSlotType.EXPLORE);
    }

    private Warning warning(short level, PlanSlotType slot, String reason) {
        return new Warning(level, slot, reason);
    }

    private record Attempt(List<Selection> selections, PlanSlotType missingSlot) {
        boolean complete() {
            return selections.size() == 3;
        }
    }

    public record Request(
            List<ScoredActivity> freshPool,
            List<ScoredActivity> preFreshnessPool,
            Set<Long> recentActivityIds,
            DevelopmentDomain periodDomain,
            Map<IntelligenceType, ChildIntelligenceScore> intelligenceScores,
            int budgetMax,
            boolean supervisedGuarantee
    ) {
        public Request {
            freshPool = List.copyOf(freshPool);
            preFreshnessPool = List.copyOf(preFreshnessPool);
            recentActivityIds = Set.copyOf(recentActivityIds);
            intelligenceScores = Map.copyOf(intelligenceScores);
        }
    }

    public record Selection(
            PlanSlotType slot,
            ScoredActivity activity,
            boolean withinBudget,
            boolean repeatNotice
    ) {
    }

    public record Warning(short fallbackLevel, PlanSlotType missingSlot, String reason) {
    }

    public record Result(
            List<Selection> selections,
            short fallbackLevel,
            int committedDurationMinutes,
            int totalDurationMinutes,
            List<Warning> warnings
    ) {
    }
}
