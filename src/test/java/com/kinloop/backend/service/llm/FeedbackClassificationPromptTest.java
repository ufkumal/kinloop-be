package com.kinloop.backend.service.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class FeedbackClassificationPromptTest {
    @Test
    void containsAllThirtyTwoFewShotExamples() {
        var matcher = Pattern.compile("\\*\\*Ö\\d+\\*\\*")
                .matcher(FeedbackClassificationPrompt.SYSTEM_PROMPT);
        int count = 0;
        while (matcher.find()) count++;
        assertEquals(32, count);
    }
}
