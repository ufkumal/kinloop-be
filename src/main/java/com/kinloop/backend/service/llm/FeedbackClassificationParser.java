package com.kinloop.backend.service.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinloop.backend.entity.enums.DifficultyHint;
import com.kinloop.backend.entity.enums.DurationHint;
import com.kinloop.backend.entity.enums.IntelligenceType;
import com.kinloop.backend.entity.enums.InvolvementHint;
import com.kinloop.backend.entity.enums.SensoryHint;
import com.kinloop.backend.entity.enums.SituationHint;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Validation rules per Kidloop_FewShot_Prompt_v2.md §10. */
@Component
public class FeedbackClassificationParser {
    private final ObjectMapper objectMapper;

    public FeedbackClassificationParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedClassification parse(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return ParsedClassification.invalid();
        }
        JsonNode node;
        try {
            node = objectMapper.readTree(rawJson);
        } catch (JsonProcessingException e) {
            return ParsedClassification.invalid();
        }
        if (!node.isObject()) {
            return ParsedClassification.invalid();
        }

        BigDecimal confidence = readConfidence(node);
        if (confidence == null
                || confidence.compareTo(BigDecimal.ZERO) < 0
                || confidence.compareTo(BigDecimal.ONE) > 0) {
            return ParsedClassification.invalid();
        }

        IntelligenceType targetCorrection = readEnum(node, "target_correction", IntelligenceType.class);
        IntelligenceType secondaryHint = readEnum(node, "secondary_hint", IntelligenceType.class);
        if (targetCorrection != null && targetCorrection == secondaryHint) {
            secondaryHint = null;
        }

        return new ParsedClassification(
                true,
                confidence,
                targetCorrection,
                secondaryHint,
                readEnum(node, "sensory_hint", SensoryHint.class),
                readEnum(node, "involvement_hint", InvolvementHint.class),
                readEnum(node, "difficulty_hint", DifficultyHint.class),
                readEnum(node, "situation_hint", SituationHint.class),
                readEnum(node, "duration_hint", DurationHint.class),
                node.path("conflict").asBoolean(false));
    }

    private BigDecimal readConfidence(JsonNode node) {
        JsonNode value = node.path("confidence");
        if (!value.isNumber()) return null;
        return BigDecimal.valueOf(value.asDouble());
    }

    private <E extends Enum<E>> E readEnum(JsonNode node, String field, Class<E> type) {
        JsonNode value = node.path(field);
        if (!value.isTextual()) return null;
        try {
            return Enum.valueOf(type, value.asText());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
