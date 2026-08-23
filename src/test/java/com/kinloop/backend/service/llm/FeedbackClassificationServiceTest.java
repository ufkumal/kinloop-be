package com.kinloop.backend.service.llm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.ParentProfile;
import com.kinloop.backend.entity.enums.ConsentType;
import com.kinloop.backend.entity.enums.FeedbackType;
import com.kinloop.backend.repository.ChildRepository;
import com.kinloop.backend.repository.ParentProfileRepository;
import com.kinloop.backend.service.ConsentService;
import com.kinloop.backend.service.matching.MatchingParameters;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedbackClassificationServiceTest {
    @Mock private ChildRepository childRepository;
    @Mock private ParentProfileRepository parentProfileRepository;
    @Mock private ConsentService consentService;
    @Mock private LlmProperties llmProperties;
    @Mock private AnthropicMessagesClient client;
    @Mock private FeedbackClassificationParser parser;
    @Mock private MatchingParameters matchingParameters;
    @Mock private SecondhandReportDetector secondhandReportDetector;
    @InjectMocks private FeedbackClassificationService service;

    private final Activity activity = new Activity();

    @BeforeEach
    void setUp() {
        Child child = new Child();
        child.setId(7L);
        child.setParentId(3L);
        ParentProfile parent = new ParentProfile();
        parent.setId(3L);
        parent.setUserId(1L);
        org.springframework.test.util.ReflectionTestUtils.setField(activity, "title", "Yaprak tepsisi");
        org.springframework.test.util.ReflectionTestUtils.setField(
                activity, "targetIntelligence",
                com.kinloop.backend.entity.enums.IntelligenceType.NATURALISTIC);
        org.mockito.Mockito.lenient().when(childRepository.findById(7L)).thenReturn(Optional.of(child));
        org.mockito.Mockito.lenient().when(parentProfileRepository.findById(3L)).thenReturn(Optional.of(parent));
        org.mockito.Mockito.lenient().when(consentService.hasGrantedConsent(1L, ConsentType.DATA_PROCESSING))
                .thenReturn(true);
    }

    @Test
    void providerFailureIsLoggedAndFailsOpen() {
        when(llmProperties.isEnabled()).thenReturn(true);
        when(client.complete(any(), any())).thenThrow(new IllegalStateException("timeout"));

        FeedbackClassificationOutcome outcome = assertDoesNotThrow(() -> service.classify(
                7L, activity, FeedbackType.LIKED, "Hikaye anlattı"));

        assertFalse(outcome.attempted());
    }

    @Test
    void disabledLlmDoesNotCallProvider() {
        when(llmProperties.isEnabled()).thenReturn(false);

        FeedbackClassificationOutcome outcome = service.classify(
                7L, activity, FeedbackType.LIKED, "Hikaye anlattı");

        assertFalse(outcome.attempted());
        verify(client, never()).complete(any(), any());
    }

    @Test
    void invalidProviderJsonIsReturnedForUnappliedAuditStorage() {
        when(llmProperties.isEnabled()).thenReturn(true);
        when(client.complete(any(), any())).thenReturn("not json");
        when(parser.parse("not json")).thenReturn(ParsedClassification.invalid());
        when(llmProperties.getModel()).thenReturn("claude-haiku-4-5");

        FeedbackClassificationOutcome outcome = service.classify(
                7L, activity, FeedbackType.LIKED, "Hikaye anlattı");

        assertTrue(outcome.attempted());
        assertFalse(outcome.classification().valid());
        assertEquals("not json", outcome.rawResponse());
    }

    @Test
    void secondhandReportConfidenceIsCappedInCode() {
        ParsedClassification parsed = new ParsedClassification(
                true, new BigDecimal("0.90"), null, null, null, null, null, null, null, false);
        when(llmProperties.isEnabled()).thenReturn(true);
        when(client.complete(any(), any())).thenReturn("{}");
        when(parser.parse("{}")).thenReturn(parsed);
        when(secondhandReportDetector.isSecondhand(any())).thenReturn(true);
        when(matchingParameters.load()).thenReturn(Map.of(
                "llm_secondhand_confidence_cap", new BigDecimal("0.60")));

        FeedbackClassificationOutcome outcome = service.classify(
                7L, activity, FeedbackType.LIKED,
                "Babası yaptırmış, çok beğenmiş diyor, ben görmedim");

        assertEquals(0, new BigDecimal("0.60").compareTo(outcome.classification().confidence()));
    }
}
