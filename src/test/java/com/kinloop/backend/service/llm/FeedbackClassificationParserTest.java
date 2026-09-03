package com.kinloop.backend.service.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinloop.backend.entity.enums.DifficultyHint;
import com.kinloop.backend.entity.enums.DurationHint;
import com.kinloop.backend.entity.enums.IntelligenceType;
import com.kinloop.backend.entity.enums.SensoryHint;
import com.kinloop.backend.entity.enums.SituationHint;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FeedbackClassificationParserTest {
    private final FeedbackClassificationParser parser = new FeedbackClassificationParser(new ObjectMapper());

    @Test
    void unparseableJsonIsInvalid() {
        ParsedClassification result = parser.parse("not json");
        assertFalse(result.valid());
    }

    @Test
    void nullOrBlankResponseIsInvalid() {
        assertFalse(parser.parse(null).valid());
        assertFalse(parser.parse("  ").valid());
    }

    @Test
    void missingConfidenceIsInvalid() {
        ParsedClassification result = parser.parse("""
                {"target_correction":null,"secondary_hint":null,"sensory_hint":null,
                 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,
                 "duration_hint":null,"conflict":false}
                """);
        assertFalse(result.valid());
    }

    @Test
    void confidenceOutsideZeroToOneIsInvalid() {
        assertFalse(parser.parse(withConfidence("1.5")).valid());
        assertFalse(parser.parse(withConfidence("-0.1")).valid());
    }

    @Test
    void confidenceAtBoundsIsValid() {
        assertTrue(parser.parse(withConfidence("0.0")).valid());
        assertTrue(parser.parse(withConfidence("1.0")).valid());
    }

    @Test
    void missingFieldsAreTreatedAsNull() {
        ParsedClassification result = parser.parse("""
                {"confidence": 0.80}
                """);
        assertTrue(result.valid());
        assertNull(result.targetCorrection());
        assertNull(result.sensoryHint());
        assertFalse(result.conflict());
    }

    @Test
    void unrecognizedEnumValueIsCoercedToNull() {
        ParsedClassification result = parser.parse("""
                {"target_correction":null,"secondary_hint":null,"sensory_hint":"PHYSICAL",
                 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,
                 "duration_hint":null,"conflict":false,"confidence":0.85}
                """);
        assertTrue(result.valid());
        assertNull(result.sensoryHint());
    }

    @Test
    void recognizedEnumValuesAreParsed() {
        ParsedClassification result = parser.parse("""
                {"target_correction":"MUSICAL","secondary_hint":null,"sensory_hint":"CROWDING",
                 "involvement_hint":null,"difficulty_hint":null,"situation_hint":"TRANSIENT",
                 "duration_hint":null,"conflict":false,"confidence":0.85}
                """);
        assertTrue(result.valid());
        assertEquals(IntelligenceType.MUSICAL, result.targetCorrection());
        assertEquals(SensoryHint.CROWDING, result.sensoryHint());
        assertEquals(SituationHint.TRANSIENT, result.situationHint());
        assertEquals(0, new BigDecimal("0.85").compareTo(result.confidence()));
    }

    @Test
    void markdownJsonCodeFenceIsUnwrappedBeforeParsing() {
        ParsedClassification result = parser.parse("""
                ```json
                {"target_correction":null,"secondary_hint":null,"sensory_hint":null,
                 "involvement_hint":null,"difficulty_hint":"HARDER","situation_hint":null,
                 "duration_hint":"SHORT","conflict":false,"confidence":0.85}
                ```
                """);

        assertTrue(result.valid());
        assertEquals(DifficultyHint.HARDER, result.difficultyHint());
        assertEquals(DurationHint.SHORT, result.durationHint());
        assertEquals(0, new BigDecimal("0.85").compareTo(result.confidence()));
    }

    @Test
    void unlabeledMarkdownCodeFenceIsUnwrappedBeforeParsing() {
        ParsedClassification result = parser.parse("""
                ```
                {"difficulty_hint":"EASIER","confidence":0.90}
                ```
                """);

        assertTrue(result.valid());
        assertEquals(DifficultyHint.EASIER, result.difficultyHint());
        assertEquals(0, new BigDecimal("0.90").compareTo(result.confidence()));
    }

    @Test
    void nonJsonMarkdownCodeFenceIsInvalid() {
        assertFalse(parser.parse("""
                ```javascript
                {"confidence":0.85}
                ```
                """).valid());
    }

    @Test
    void identicalTargetAndSecondaryNullsOutSecondary() {
        ParsedClassification result = parser.parse("""
                {"target_correction":"MUSICAL","secondary_hint":"MUSICAL","sensory_hint":null,
                 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,
                 "duration_hint":null,"conflict":false,"confidence":0.85}
                """);
        assertEquals(IntelligenceType.MUSICAL, result.targetCorrection());
        assertNull(result.secondaryHint());
    }

    @Test
    void conflictFlagIsParsed() {
        ParsedClassification result = parser.parse("""
                {"target_correction":null,"secondary_hint":null,"sensory_hint":null,
                 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,
                 "duration_hint":null,"conflict":true,"confidence":0.80}
                """);
        assertTrue(result.conflict());
    }

    private String withConfidence(String confidence) {
        return """
                {"target_correction":null,"secondary_hint":null,"sensory_hint":null,
                 "involvement_hint":null,"difficulty_hint":null,"situation_hint":null,
                 "duration_hint":null,"conflict":false,"confidence":%s}
                """.formatted(confidence);
    }
}
