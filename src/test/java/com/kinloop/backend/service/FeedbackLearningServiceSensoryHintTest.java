package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.kinloop.backend.entity.ChildSensoryAdjustment;
import com.kinloop.backend.entity.enums.InvolvementFilter;
import com.kinloop.backend.entity.enums.InvolvementHint;
import com.kinloop.backend.entity.enums.SensoryHint;
import com.kinloop.backend.repository.ChildSensoryAdjustmentRepository;
import com.kinloop.backend.service.matching.MatchingParameters;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FeedbackLearningServiceSensoryHintTest {
    private final AtomicReference<ChildSensoryAdjustment> stored = new AtomicReference<>();
    private FeedbackLearningService service;

    @BeforeEach
    void setUp() {
        ChildSensoryAdjustmentRepository repository = (ChildSensoryAdjustmentRepository) Proxy.newProxyInstance(
                ChildSensoryAdjustmentRepository.class.getClassLoader(),
                new Class<?>[]{ChildSensoryAdjustmentRepository.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("findByChildId")) {
                        return Optional.ofNullable(stored.get());
                    }
                    if (method.getName().equals("save")) {
                        stored.set((ChildSensoryAdjustment) arguments[0]);
                        return arguments[0];
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        MatchingParameters parameters = new MatchingParameters(null) {
            @Override
            public Map<String, BigDecimal> load() {
                return Map.of("llm_sensory_tolerance_step", new BigDecimal("1.00"));
            }
        };
        service = new FeedbackLearningService(
                null, null, null, null, null, null, null, parameters, repository, null);
    }

    @Test
    void createsAdjustmentOnFirstSensoryHint() {
        service.applySensoryHint(7L, SensoryHint.NOISE);

        ChildSensoryAdjustment adjustment = stored.get();
        assertEquals((short) -1, adjustment.getNoiseAdjustment());
        assertEquals((short) 0, adjustment.getVisualAdjustment());
    }

    @Test
    void accumulatesOnExistingAdjustment() {
        stored.set(new ChildSensoryAdjustment(7L, (short) -1, (short) 0, (short) 0, null));

        service.applySensoryHint(7L, SensoryHint.NOISE);

        assertEquals((short) -2, stored.get().getNoiseAdjustment());
    }

    @Test
    void crowdingTightensNoiseAndVisual() {
        service.applySensoryHint(7L, SensoryHint.CROWDING);

        ChildSensoryAdjustment adjustment = stored.get();
        assertEquals((short) -1, adjustment.getNoiseAdjustment());
        assertEquals((short) -1, adjustment.getVisualAdjustment());
    }

    @Test
    void togetherHintSetsStrictFilterAndPreservesSensoryAdjustments() {
        stored.set(new ChildSensoryAdjustment(7L, (short) -2, (short) 0, (short) 0, null));

        service.applyInvolvementHint(7L, InvolvementHint.TOGETHER);

        ChildSensoryAdjustment adjustment = stored.get();
        assertEquals(InvolvementFilter.STRICT, adjustment.getInvolvementFilter());
        assertEquals((short) -2, adjustment.getNoiseAdjustment());
    }

    @Test
    void aloneHintCreatesAdjustmentWithRelaxedFilterWhenNoneExists() {
        assertNull(stored.get());

        service.applyInvolvementHint(7L, InvolvementHint.ALONE);

        assertEquals(InvolvementFilter.RELAXED, stored.get().getInvolvementFilter());
    }
}
