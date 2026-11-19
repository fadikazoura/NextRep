package com.fadikazoura.nextrep.plan;

import com.fadikazoura.nextrep.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TrainingPlanIntegrationTest {

    private static final String URL = "/api/users/me/plan";

    private static final String PROFILE = """
            {
              "birthDate": "2002-03-14",
              "sex": "MALE",
              "heightCm": 178,
              "experienceLevel": "INTERMEDIATE",
              "goal": "HYPERTROPHY",
              "sessionMinutes": 60,
              "equipment": "FULL_GYM",
              "weightUnit": "KG",
              "trainingDays": ["SUNDAY", "TUESDAY", "THURSDAY"]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrainingPlanRepository trainingPlanRepository;

    @Test
    void returnsNotFoundBeforeAnyPlanExists() throws Exception {
        String token = registerAndLogin("plan-none@example.com");

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Plan not found"));
    }

    @Test
    void cannotGeneratePlanWithoutProfile() throws Exception {
        String token = registerAndLogin("plan-noprofile@example.com");

        generate(token)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Profile not found"));
    }

    @Test
    void generatesPlanFromProfile() throws Exception {
        String token = registerAndLoginWithProfile("plan-generate@example.com");

        generate(token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("DEFAULT"))
                .andExpect(jsonPath("$.name").value("Full Body - 3 days"))
                .andExpect(jsonPath("$.days[*].dayOfWeek", contains("TUESDAY", "THURSDAY", "SUNDAY")))
                .andExpect(jsonPath("$.days[0].name").value("Full Body"))
                .andExpect(jsonPath("$.days[0].exercises", hasSize(6)))
                .andExpect(jsonPath("$.days[0].exercises[0].position").value(1))
                .andExpect(jsonPath("$.days[0].exercises[0].name").isNotEmpty())
                .andExpect(jsonPath("$.days[0].exercises[0].repMin").value(8))
                .andExpect(jsonPath("$.days[0].exercises[0].repMax").value(12));
    }

    @Test
    void returnsActivePlanAfterGeneration() throws Exception {
        String token = registerAndLoginWithProfile("plan-get@example.com");
        Integer planId = JsonPath.read(generate(token).andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(planId))
                .andExpect(jsonPath("$.days", hasSize(3)));
    }

    @Test
    void regeneratingReplacesActivePlanAndKeepsOldOneAsHistory() throws Exception {
        String token = registerAndLoginWithProfile("plan-regenerate@example.com");
        Integer firstId = JsonPath.read(generate(token).andReturn().getResponse().getContentAsString(), "$.id");

        Integer secondId = JsonPath.read(generate(token)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");

        assertThat(secondId).isNotEqualTo(firstId);
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.id").value(secondId));
        assertThat(trainingPlanRepository.findById(firstId.longValue()))
                .hasValueSatisfying(oldPlan -> assertThat(oldPlan.isActive()).isFalse());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions generate(String token) throws Exception {
        return mockMvc.perform(post(URL + "/generate").header("Authorization", "Bearer " + token));
    }

    private String registerAndLoginWithProfile(String email) throws Exception {
        String token = registerAndLogin(email);
        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROFILE))
                .andExpect(status().isOk());
        return token;
    }

    private String registerAndLogin(String email) throws Exception {
        String credentials = """
                {"email": "%s", "password": "supersecret123"}
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isCreated());

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$.accessToken");
    }
}
