package com.kinloop.backend.service.matching;

import com.kinloop.backend.entity.*;
import com.kinloop.backend.entity.enums.*;
import com.kinloop.backend.repository.*;

import java.math.BigDecimal;
import java.util.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchingStateInitializer {
    private final ChildIntelligenceScoreRepository intelligenceRepository;
    private final ChildDomainLevelRepository domainRepository;

    public void initialize(Long childId, int ageMonths, List<GardnerPrior> priors, Map<String, BigDecimal> p) {
        if (!intelligenceRepository.existsByChildId(childId)) {
            Map<IntelligenceType, ChildIntelligenceScore> scores = new EnumMap<>(IntelligenceType.class);
            for (IntelligenceType type : IntelligenceType.values())
                scores.put(type, new ChildIntelligenceScore(childId, type, p.get("gardner_initial_score")));
            if (priors != null) for (GardnerPrior prior : priors)
                scores.get(prior.domain()).applyPrior(BigDecimal.valueOf(prior.delta()), p.get("gardner_prior_min_score"), p.get("gardner_prior_max_score"));
            intelligenceRepository.saveAll(scores.values());
        }
        Set<DevelopmentDomain> existing = domainRepository.findByChildId(childId).stream().map(ChildDomainLevel::getDomain).collect(java.util.stream.Collectors.toSet());
        short initialLevel = initialDomainLevel(ageMonths, p);
        List<ChildDomainLevel> missing = Arrays.stream(DevelopmentDomain.values())
                .filter(domain -> !existing.contains(domain))
                .map(domain -> new ChildDomainLevel(childId, domain, initialLevel))
                .toList();
        domainRepository.saveAll(missing);
    }

    short initialDomainLevel(int ageMonths, Map<String, BigDecimal> parameters) {
        if (ageMonths < 0 || ageMonths >= 73) {
            throw new IllegalArgumentException("Age must be between 0 and 72 months");
        }
        if (ageMonths < 48) return parameters.get("domain_initial_level_under_48m").shortValueExact();
        if (ageMonths < 60) return parameters.get("domain_initial_level_48_to_60m").shortValueExact();
        return parameters.get("domain_initial_level_60_to_73m").shortValueExact();
    }
}
