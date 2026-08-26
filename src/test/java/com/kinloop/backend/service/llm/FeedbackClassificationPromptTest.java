package com.kinloop.backend.service.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class FeedbackClassificationPromptTest {
    @Test
    void containsAllFortyOneV4FewShotExamples() {
        var matcher = Pattern.compile("\\*\\*Ö\\d+\\*\\*")
                .matcher(FeedbackClassificationPrompt.SYSTEM_PROMPT);
        int count = 0;
        while (matcher.find()) count++;
        assertEquals(41, count);
    }

    @Test
    void containsV4SafetyRules() {
        String prompt = FeedbackClassificationPrompt.SYSTEM_PROMPT;
        org.junit.jupiter.api.Assertions.assertTrue(prompt.contains("YALANLAMA -> target_correction dolar"));
        org.junit.jupiter.api.Assertions.assertTrue(prompt.contains("confidence en fazla 0.65"));
        org.junit.jupiter.api.Assertions.assertTrue(prompt.contains("sağlık, gelişim geriliği"));
        org.junit.jupiter.api.Assertions.assertTrue(prompt.contains("Olumsuz duygusal tepki"));
        org.junit.jupiter.api.Assertions.assertTrue(prompt.contains("Kâğıtları fırlattı, ağladı, çok öfkelendi"));
        org.junit.jupiter.api.Assertions.assertTrue(prompt.contains("doktor da gelişim geriliği olabilir dedi"));
    }
}
