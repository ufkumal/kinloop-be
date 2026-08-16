package com.kinloop.backend.service.matching;

import com.kinloop.backend.entity.enums.PlanSlotType;

import java.math.BigDecimal;
import java.util.*;

import org.springframework.stereotype.Component;

@Component
public class DailyPortfolioBuilder {
    private final Random random;

    public DailyPortfolioBuilder() {
        this(new Random());
    }

    DailyPortfolioBuilder(Random random) {
        this.random = random;
    }

    public record Selection(PlanSlotType slot, ScoredActivity activity) {
    }

    public List<Selection> build(List<ScoredActivity> develop, List<ScoredActivity> strengthen, List<ScoredActivity> explore, int budget) {
        List<List<Selection>> variants = new ArrayList<>();
        for (var a : develop)
            for (var b : strengthen)
                for (var c : explore)
                    if (distinct(a, b, c))
                        variants.add(List.of(new Selection(PlanSlotType.DEVELOP, a), new Selection(PlanSlotType.STRENGTHEN, b), new Selection(PlanSlotType.EXPLORE, c)));
        var best = best(variants, budget);
        if (!best.isEmpty()) return best;
        variants.clear();
        for (var a : develop)
            for (var b : strengthen)
                if (!a.activity().getId().equals(b.activity().getId()))
                    variants.add(List.of(new Selection(PlanSlotType.DEVELOP, a), new Selection(PlanSlotType.STRENGTHEN, b)));
        best = best(variants, budget);
        if (!best.isEmpty()) return best;
        return develop.stream().filter(a -> a.activity().getDurationMinutes() <= budget).findFirst().map(a -> List.of(new Selection(PlanSlotType.DEVELOP, a))).orElse(List.of());
    }

    private List<Selection> best(List<List<Selection>> values, int budget) {
        List<List<Selection>> fitting = values.stream()
                .filter(variant -> variant.stream()
                        .mapToInt(selection -> selection.activity().activity().getDurationMinutes())
                        .sum() <= budget)
                .toList();
        if (fitting.isEmpty()) return List.of();

        BigDecimal maximum = fitting.stream()
                .map(this::totalScore)
                .max(BigDecimal::compareTo)
                .orElseThrow();
        List<List<Selection>> tied = fitting.stream()
                .filter(variant -> totalScore(variant).compareTo(maximum) == 0)
                .toList();

        // v5 doc §7.1 Eşitlik durumu: equally scored alternatives are resolved by lottery.
        return tied.get(random.nextInt(tied.size()));
    }

    private BigDecimal totalScore(List<Selection> variant) {
        return variant.stream()
                .map(selection -> selection.activity().rawScore())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean distinct(ScoredActivity a, ScoredActivity b, ScoredActivity c) {
        return !a.activity().getId().equals(b.activity().getId()) && !a.activity().getId().equals(c.activity().getId()) && !b.activity().getId().equals(c.activity().getId());
    }
}
