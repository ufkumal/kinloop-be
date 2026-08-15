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

    public void initialize(Long childId, List<GardnerPrior> priors, Map<String, BigDecimal> p) {
        if (!intelligenceRepository.existsByChildId(childId)) {
            Map<IntelligenceType, ChildIntelligenceScore> scores = new EnumMap<>(IntelligenceType.class);
            for (IntelligenceType type : IntelligenceType.values())
                scores.put(type, new ChildIntelligenceScore(childId, type, p.get("gardner_initial_score")));
            if (priors != null) for (GardnerPrior prior : priors)
                scores.get(prior.domain()).applyPrior(BigDecimal.valueOf(prior.delta()), p.get("gardner_prior_min_score"), p.get("gardner_prior_max_score"));
            intelligenceRepository.saveAll(scores.values());
        }
        Set<DevelopmentDomain> existing = domainRepository.findByChildId(childId).stream().map(ChildDomainLevel::getDomain).collect(java.util.stream.Collectors.toSet());
        List<ChildDomainLevel> missing = Arrays.stream(DevelopmentDomain.values()).filter(d -> !existing.contains(d)).map(d -> new ChildDomainLevel(childId, d, p.get("domain_initial_level").shortValue())).toList();
        domainRepository.saveAll(missing);
    }
}
