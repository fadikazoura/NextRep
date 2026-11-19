package com.fadikazoura.nextrep.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

/**
 * Gemini settings. The API key comes from the GEMINI_API_KEY environment variable and is never committed.
 * Without a key the AI planner is disabled and the default generator is used instead.
 */
@ConfigurationProperties("nextrep.ai.gemini")
public record GeminiProperties(String apiKey, String model, URI baseUrl, Duration timeout) {

    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
