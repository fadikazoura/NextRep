package com.fadikazoura.nextrep.user;

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

import java.time.LocalDate;

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
class ProfileIntegrationTest {

    private static final String PASSWORD = "supersecret123";

    private static final String VALID_PROFILE = """
            {
              "birthDate": "2003-05-10",
              "sex": "MALE",
              "heightCm": 180,
              "experienceLevel": "INTERMEDIATE",
              "goal": "HYPERTROPHY",
              "sessionMinutes": 60,
              "equipment": "FULL_GYM",
              "weightUnit": "KG",
              "trainingDays": ["THURSDAY", "SUNDAY", "TUESDAY"]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsNotFoundBeforeProfileIsCreated() throws Exception {
        String token = registerAndLogin("noprofile@example.com");

        mockMvc.perform(get("/api/users/me/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Profile not found"));
    }

    @Test
    void createsProfileAndReturnsTrainingDaysInWeekOrder() throws Exception {
        String token = registerAndLogin("create@example.com");

        putProfile(token, VALID_PROFILE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goal").value("HYPERTROPHY"))
                .andExpect(jsonPath("$.weightUnit").value("KG"))
                .andExpect(jsonPath("$.trainingDays", contains("TUESDAY", "THURSDAY", "SUNDAY")));

        mockMvc.perform(get("/api/users/me/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heightCm").value(180));
    }

    @Test
    void updatesExistingProfile() throws Exception {
        String token = registerAndLogin("update@example.com");
        putProfile(token, VALID_PROFILE).andExpect(status().isOk());

        String updated = VALID_PROFILE
                .replace("\"HYPERTROPHY\"", "\"STRENGTH\"")
                .replace("[\"THURSDAY\", \"SUNDAY\", \"TUESDAY\"]", "[\"MONDAY\"]");

        putProfile(token, updated)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goal").value("STRENGTH"))
                .andExpect(jsonPath("$.trainingDays", hasSize(1)));
    }

    @Test
    void rejectsUserYoungerThanMinimumAge() throws Exception {
        String token = registerAndLogin("young@example.com");
        String tooYoung = VALID_PROFILE.replace("2003-05-10", LocalDate.now().minusYears(10).toString());

        putProfile(token, tooYoung)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid profile"));
    }

    @Test
    void rejectsInvalidFieldsWithFieldErrors() throws Exception {
        String token = registerAndLogin("invalid@example.com");
        String invalid = VALID_PROFILE
                .replace("\"heightCm\": 180", "\"heightCm\": 50")
                .replace("[\"THURSDAY\", \"SUNDAY\", \"TUESDAY\"]", "[]");

        putProfile(token, invalid)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.heightCm").exists())
                .andExpect(jsonPath("$.errors.trainingDays").exists());
    }

    @Test
    void rejectsUnknownEnumValueWithoutLeakingDetails() throws Exception {
        String token = registerAndLogin("enum@example.com");

        putProfile(token, VALID_PROFILE.replace("\"MALE\"", "\"ALIEN\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PROFILE))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions putProfile(String token, String body) throws Exception {
        return mockMvc.perform(put("/api/users/me/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String registerAndLogin(String email) throws Exception {
        String credentials = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, PASSWORD);

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