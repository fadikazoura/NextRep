package com.fadikazoura.nextrep.plan;

import com.fadikazoura.nextrep.TestcontainersConfiguration;
import com.fadikazoura.nextrep.ai.AiUnavailableException;
import com.fadikazoura.nextrep.ai.GeminiClient;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The real Gemini is replaced by a Mockito mock: no network, no API key, deterministic answers. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TrainingPlanAiIntegrationTest {

    private static final String PROFILE = """
            {
              "birthDate": "2000-01-15",
              "sex": "MALE",
              "heightCm": 182,
              "experienceLevel": "INTERMEDIATE",
              "goal": "STRENGTH",
              "sessionMinutes": 60,
              "equipment": "FULL_GYM",
              "weightUnit": "KG",
              "trainingDays": ["MONDAY", "WEDNESDAY", "FRIDAY"],
              "injuries": "Mild shoulder pain"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeminiClient geminiClient;

    @Test
    void savesTheAiPlanWhenTheAiAnswersWithAValidPlan() throws Exception {
        String token = newUserWithProfile();
        when(geminiClient.isEnabled()).thenReturn(true);
        when(geminiClient.generateJson(anyString(), any())).thenReturn(aiAnswer(token, null));

        generate(token, true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("AI"))
                .andExpect(jsonPath("$.name").value("Shoulder-Friendly Strength"))
                .andExpect(jsonPath("$.days", hasSize(3)))
                .andExpect(jsonPath("$.days[0].name").value("Lower Focus"))
                .andExpect(jsonPath("$.days[0].exercises[0].name").value("Barbell Back Squat"))
                .andExpect(jsonPath("$.days[0].exercises[0].repMin").value(4));
    }

    @Test
    void sendsTheInjuriesToTheAi() throws Exception {
        String token = newUserWithProfile();
        when(geminiClient.isEnabled()).thenReturn(true);
        when(geminiClient.generateJson(anyString(), any())).thenReturn(aiAnswer(token, null));
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);

        generate(token, true).andExpect(status().isCreated());

        verify(geminiClient).generateJson(prompt.capture(), any());
        assertThat(prompt.getValue()).contains("Mild shoulder pain", "Goal: STRENGTH");
    }

    @Test
    void fallsBackToTheDefaultPlanWhenTheAiIsDown() throws Exception {
        String token = newUserWithProfile();
        when(geminiClient.isEnabled()).thenReturn(true);
        when(geminiClient.generateJson(anyString(), any())).thenThrow(new AiUnavailableException("503 high demand"));

        generate(token, true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("DEFAULT"))
                .andExpect(jsonPath("$.name").value("Full Body - 3 days"));
    }

    @Test
    void fallsBackToTheDefaultPlanWhenTheAiInventsAnExercise() throws Exception {
        String token = newUserWithProfile();
        when(geminiClient.isEnabled()).thenReturn(true);
        when(geminiClient.generateJson(anyString(), any())).thenReturn(aiAnswer(token, 999999L));

        generate(token, true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("DEFAULT"));
    }

    @Test
    void usesTheDefaultGeneratorWhenTheAiIsNotConfigured() throws Exception {
        String token = newUserWithProfile();
        when(geminiClient.isEnabled()).thenReturn(false);

        generate(token, true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("DEFAULT"));
        verify(geminiClient, never()).generateJson(anyString(), any());
    }

    @Test
    void doesNotCallTheAiUnlessAsked() throws Exception {
        String token = newUserWithProfile();
        when(geminiClient.isEnabled()).thenReturn(true);

        mockMvc.perform(post("/api/users/me/plan/generate").header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("DEFAULT"));
        verify(geminiClient, never()).generateJson(anyString(), any());
    }

    private ResultActions generate(String token, boolean ai) throws Exception {
        return mockMvc.perform(post("/api/users/me/plan/generate")
                .param("ai", String.valueOf(ai))
                .header("Authorization", bearer(token)));
    }

    /** A plan the AI could return, built from real catalog ids. {@code inventedId} replaces one exercise. */
    private String aiAnswer(String token, Long inventedId) throws Exception {
        long squat = exerciseId(token, "QUADS", "BARBELL");
        long bench = exerciseId(token, "CHEST", "BARBELL");
        long row = exerciseId(token, "BACK", "BARBELL");
        long first = inventedId != null ? inventedId : squat;
        return """
                {
                  "name": "Shoulder-Friendly Strength",
                  "days": [
                    {"dayOfWeek": "MONDAY", "name": "Lower Focus",
                     "exercises": [{"exerciseId": %d, "sets": 4, "repMin": 4, "repMax": 6},
                                   {"exerciseId": %d, "sets": 3, "repMin": 6, "repMax": 8}]},
                    {"dayOfWeek": "WEDNESDAY", "name": "Upper Focus",
                     "exercises": [{"exerciseId": %d, "sets": 4, "repMin": 4, "repMax": 6},
                                   {"exerciseId": %d, "sets": 3, "repMin": 6, "repMax": 8}]},
                    {"dayOfWeek": "FRIDAY", "name": "Full Body",
                     "exercises": [{"exerciseId": %d, "sets": 4, "repMin": 4, "repMax": 6},
                                   {"exerciseId": %d, "sets": 3, "repMin": 6, "repMax": 8}]}
                  ]
                }
                """.formatted(first, row, bench, row, squat, bench);
    }

    private long exerciseId(String token, String muscleGroup, String equipment) throws Exception {
        String response = mockMvc.perform(get("/api/exercises")
                        .param("muscleGroup", muscleGroup)
                        .param("equipment", equipment)
                        .header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$[0].id")).longValue();
    }

    private String newUserWithProfile() throws Exception {
        String credentials = """
                {"email": "ai-plan-%s@example.com", "password": "supersecret123"}
                """.formatted(UUID.randomUUID());
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isCreated());
        String login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(login, "$.accessToken");

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROFILE))
                .andExpect(status().isOk());
        return token;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
