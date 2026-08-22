package com.kinloop.backend.service.matching;

import static com.kinloop.backend.service.matching.MatchingTestFixtures.parameters;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinloop.backend.entity.ChildDomainLevel;
import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.repository.ChildDomainLevelRepository;
import com.kinloop.backend.repository.ChildIntelligenceScoreRepository;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MatchingStateInitializerTest {

    @Test
    void choosesInitialLevelAtEveryAgeBoundary() {
        MatchingStateInitializer initializer = new MatchingStateInitializer(null, null);

        assertEquals((short) 1, initializer.initialDomainLevel(0, parameters()));
        assertEquals((short) 1, initializer.initialDomainLevel(47, parameters()));
        assertEquals((short) 2, initializer.initialDomainLevel(48, parameters()));
        assertEquals((short) 2, initializer.initialDomainLevel(59, parameters()));
        assertEquals((short) 3, initializer.initialDomainLevel(60, parameters()));
        assertEquals((short) 3, initializer.initialDomainLevel(72, parameters()));
    }

    @Test
    void initializesAllSevenDomainsWithTheAgeBasedLevel() {
        ChildIntelligenceScoreRepository intelligenceRepository = mock(ChildIntelligenceScoreRepository.class);
        ChildDomainLevelRepository domainRepository = mock(ChildDomainLevelRepository.class);
        when(intelligenceRepository.existsByChildId(7L)).thenReturn(true);
        when(domainRepository.findByChildId(7L)).thenReturn(List.of());
        MatchingStateInitializer initializer = new MatchingStateInitializer(
                intelligenceRepository, domainRepository);

        initializer.initialize(7L, 60, null, parameters());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<ChildDomainLevel>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(domainRepository).saveAll(captor.capture());
        assertEquals(DevelopmentDomain.values().length, captor.getValue().size());
        assertEquals(7, captor.getValue().size());
        assertEquals(List.of((short) 3), captor.getValue().stream()
                .map(ChildDomainLevel::getLevel).distinct().toList());
    }
}
