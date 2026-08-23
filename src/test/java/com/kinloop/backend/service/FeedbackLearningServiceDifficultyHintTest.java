package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kinloop.backend.entity.ChildDomainLevel;
import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.DifficultyHint;
import com.kinloop.backend.repository.ChildDomainLevelRepository;
import com.kinloop.backend.service.matching.MatchingParameters;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FeedbackLearningServiceDifficultyHintTest {
    private ChildDomainLevel targetDomain;
    private ChildDomainLevel otherDomain;
    private FeedbackLearningService service;

    @BeforeEach
    void setUp() {
        targetDomain = new ChildDomainLevel(7L, DevelopmentDomain.LANGUAGE, (short) 2);
        otherDomain = new ChildDomainLevel(7L, DevelopmentDomain.COGNITIVE, (short) 2);
        ChildDomainLevelRepository domainRepository = (ChildDomainLevelRepository) Proxy.newProxyInstance(
                ChildDomainLevelRepository.class.getClassLoader(),
                new Class<?>[]{ChildDomainLevelRepository.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("findByChildId")) {
                        return List.of(otherDomain, targetDomain);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        MatchingParameters parameters = new MatchingParameters(null) {
            @Override
            public Map<String, BigDecimal> load() {
                return parameters();
            }
        };
        service = new FeedbackLearningService(
                null, null, null, null, null, null, domainRepository, parameters);
    }

    @Test
    void harderHintAccumulatesInTheTargetDomainAndUsesTheNormalLevelUpThreshold() {
        service.applyDifficultyHint(7L, DevelopmentDomain.LANGUAGE, DifficultyHint.HARDER);
        service.applyDifficultyHint(7L, DevelopmentDomain.LANGUAGE, DifficultyHint.HARDER);

        assertEquals((short) 2, targetDomain.getLevel());
        assertDecimal("2.0", targetDomain.getStreak());

        service.applyDifficultyHint(7L, DevelopmentDomain.LANGUAGE, DifficultyHint.HARDER);

        assertEquals((short) 3, targetDomain.getLevel());
        assertDecimal("0", targetDomain.getStreak());
        assertEquals((short) 2, otherDomain.getLevel());
        assertDecimal("0", otherDomain.getStreak());
    }

    @Test
    void easierHintAccumulatesAndUsesTheNormalStrictLevelDownThreshold() {
        service.applyDifficultyHint(7L, DevelopmentDomain.LANGUAGE, DifficultyHint.EASIER);
        service.applyDifficultyHint(7L, DevelopmentDomain.LANGUAGE, DifficultyHint.EASIER);

        assertEquals((short) 2, targetDomain.getLevel());
        assertDecimal("-1.0", targetDomain.getStreak());

        service.applyDifficultyHint(7L, DevelopmentDomain.LANGUAGE, DifficultyHint.EASIER);

        assertEquals((short) 1, targetDomain.getLevel());
        assertDecimal("0", targetDomain.getStreak());
    }

    private Map<String, BigDecimal> parameters() {
        return Map.of(
                "llm_difficulty_hint_harder_delta", new BigDecimal("1.0"),
                "llm_difficulty_hint_easier_delta", new BigDecimal("-0.5"),
                "level_up_threshold", new BigDecimal("3.0"),
                "level_down_threshold", new BigDecimal("-1.0"),
                "level_min", BigDecimal.ONE,
                "level_max", new BigDecimal("4"),
                "ceiling_counter_cap", new BigDecimal("1.0"));
    }

    private void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
