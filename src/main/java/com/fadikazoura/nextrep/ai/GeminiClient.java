package com.fadikazoura.nextrep.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/** Thin client for the Gemini generateContent REST API with structured (JSON) output. */
@Slf4j
@Component
public class GeminiClient {

    private final RestClient restClient;
    private final GeminiProperties properties;

    public GeminiClient(RestClient geminiRestClient, GeminiProperties properties) {
        this.restClient = geminiRestClient;
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.enabled();
    }

    /**
     * Sends the prompt and returns the model's answer as JSON text that follows the given JSON Schema.
     *
     * @throws AiUnavailableException if the AI is not configured, fails, times out or returns nothing
     */
    public String generateJson(String prompt, Map<String, Object> jsonSchema) {
        if (!properties.enabled()) {
            throw new AiUnavailableException("Gemini API key is not configured");
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseJsonSchema", jsonSchema));

        try {
            GenerateContentResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", properties.model())
                    .header("x-goog-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GenerateContentResponse.class);
            return extractText(response);
        } catch (RestClientException e) {
            // Never log the request headers: they contain the API key.
            log.warn("Gemini request to model {} failed: {} (root cause: {})",
                    properties.model(), e.getMessage(), e.getMostSpecificCause().toString());
            throw new AiUnavailableException("Gemini request failed", e);
        }
    }

    /**
     * A conversation: fixed instructions for the model (the "system instruction", which the user cannot see or
     * change) plus the turns so far, ending with the user's question. Returns the model's plain-text answer.
     *
     * @throws AiUnavailableException if the AI is not configured, fails, times out or returns nothing
     */
    public String generateText(String systemInstruction, List<ChatTurn> turns, double temperature, int maxOutputTokens) {
        if (!properties.enabled()) {
            throw new AiUnavailableException("Gemini API key is not configured");
        }

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                "contents", turns.stream()
                        .map(turn -> Map.of("role", turn.fromUser() ? "user" : "model",
                                "parts", List.of(Map.of("text", turn.text()))))
                        .toList(),
                "generationConfig", Map.of("temperature", temperature, "maxOutputTokens", maxOutputTokens));

        try {
            GenerateContentResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", properties.model())
                    .header("x-goog-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GenerateContentResponse.class);
            return extractText(response);
        } catch (RestClientException e) {
            log.warn("Gemini chat request to model {} failed: {} (root cause: {})",
                    properties.model(), e.getMessage(), e.getMostSpecificCause().toString());
            throw new AiUnavailableException("Gemini request failed", e);
        }
    }

    /** One message of a conversation; {@code fromUser} is false for the model's earlier answers. */
    public record ChatTurn(boolean fromUser, String text) {
    }

    private static String extractText(GenerateContentResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new AiUnavailableException("Gemini returned no candidates");
        }
        Content content = response.candidates().getFirst().content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            throw new AiUnavailableException("Gemini returned an empty answer");
        }
        String text = content.parts().getFirst().text();
        if (text == null || text.isBlank()) {
            throw new AiUnavailableException("Gemini returned an empty answer");
        }
        return text;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GenerateContentResponse(List<Candidate> candidates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(Content content, String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(List<Part> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(String text) {
    }
}
