package com.kinloop.backend.service.matching;

import com.kinloop.backend.entity.enums.PlanSlotType;

import java.math.BigDecimal;
import java.util.*;

import org.springframework.stereotype.Component;

@Component
public class DailyPortfolioBuilder {
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
        return values.stream().filter(v -> v.stream().mapToInt(x -> x.activity().activity().getDurationMinutes()).sum() <= budget).max(Comparator.comparing(v -> v.stream().map(x -> x.activity().rawScore()).reduce(BigDecimal.ZERO, BigDecimal::add))).orElse(List.of());
    }

    private boolean distinct(ScoredActivity a, ScoredActivity b, ScoredActivity c) {
        return !a.activity().getId().equals(b.activity().getId()) && !a.activity().getId().equals(c.activity().getId()) && !b.activity().getId().equals(c.activity().getId());
    }
}
