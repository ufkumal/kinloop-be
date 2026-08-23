package com.kinloop.backend.service.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LlmProperties {
    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final int maxTokens;

    public LlmProperties(
            @Value("${llm.enabled}") boolean enabled,
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.model}") String model,
            @Value("${llm.temperature}") double temperature,
            @Value("${llm.max-tokens}") int maxTokens
    ) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }
}
