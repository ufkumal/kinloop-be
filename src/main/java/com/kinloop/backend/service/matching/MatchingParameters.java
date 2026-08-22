package com.kinloop.backend.service.matching;

import com.kinloop.backend.entity.ScoringParameter;
import com.kinloop.backend.repository.ScoringParameterRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchingParameters {
    private final ScoringParameterRepository repository;

    public Map<String, BigDecimal> load() {
        return repository.findAll().stream().collect(Collectors.toUnmodifiableMap(ScoringParameter::getKey, ScoringParameter::getValue));
    }
}
