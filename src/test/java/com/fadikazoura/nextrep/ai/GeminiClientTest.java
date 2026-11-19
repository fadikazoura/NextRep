package com.fadikazoura.nextrep.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Gemini is replaced by a mock HTTP server: tests never call the real API. */
class GeminiClientTest {

    private static final String BASE_URL = "https://gemini.test";
    private static final String ENDPOINT = BASE_URL + "/v1beta/models/gemini-test:generateContent";
    private static final Map<String, Object> SCHEMA = Map.of("type", "object");

    private MockRestServiceServer server;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    @Test
    void sendsPromptWithJsonSchemaAndReturnsTheAnswerText() {
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("Build a plan"))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.generationConfig.responseJsonSchema.type").value("object"))
                .andRespond(withSuccess("""
                        {"candidates": [{"content": {"parts": [{"text": "{\\"name\\": \\"AI plan\\"}"}]},
                                         "finishReason": "STOP"}],
                         "usageMetadata": {"totalTokenCount": 42}}
                        """, MediaType.APPLICATION_JSON));

        String answer = client("test-key").generateJson("Build a plan", SCHEMA);

        assertThat(answer).isEqualTo("{\"name\": \"AI plan\"}");
        server.verify();
    }

    @Test
    void overloadedModelBecomesAiUnavailable() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client("test-key").generateJson("Build a plan", SCHEMA))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void answerWithoutCandidatesBecomesAiUnavailable() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess("{\"candidates\": []}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client("test-key").generateJson("Build a plan", SCHEMA))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("no candidates");
    }

    @Test
    void missingApiKeyFailsFastWithoutCallingGemini() {
        GeminiClient client = client("");

        assertThat(client.isEnabled()).isFalse();
        assertThatThrownBy(() -> client.generateJson("Build a plan", SCHEMA))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("not configured");
        server.verify();
    }

    private GeminiClient client(String apiKey) {
        return new GeminiClient(restClient,
                new GeminiProperties(apiKey, "gemini-test", URI.create(BASE_URL), Duration.ofSeconds(5)));
    }
}
