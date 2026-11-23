package com.fadikazoura.nextrep.insights;

import com.fadikazoura.nextrep.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class InsightsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aNewUserHasNothingYet() throws Exception {
        String email = "insights-" + System.nanoTime() + "@example.com";
        mockMvc.perform(post("/api/auth/register").contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"supersecret123\"}"))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"login\":\"" + email + "\",\"password\":\"supersecret123\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");

        mockMvc.perform(get("/api/users/me/insights").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streakWeeks").value(0))
                .andExpect(jsonPath("$.weeklyGoal").value(0))
                .andExpect(jsonPath("$.achievements", hasSize(Achievement.values().length)))
                .andExpect(jsonPath("$.achievements[?(@.unlocked == true)]", hasSize(0)));
    }

    @Test
    void theDemoHistoryHasAStreakAndBadges() throws Exception {
        String body = mockMvc.perform(post("/api/auth/demo")).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");

        mockMvc.perform(get("/api/users/me/insights?tz=Asia/Jerusalem&firstDay=SUNDAY")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyGoal").value(4))
                .andExpect(jsonPath("$.longestStreakWeeks", greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.lastWeek.workouts", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.achievements[?(@.unlocked == true)].code", hasItem("WORKOUTS_10")));
    }

    @Test
    void anUnknownTimeZoneFallsBackToUtc() throws Exception {
        String body = mockMvc.perform(post("/api/auth/demo")).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");

        mockMvc.perform(get("/api/users/me/insights?tz=Not/AZone").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void requiresAToken() throws Exception {
        mockMvc.perform(get("/api/users/me/insights")).andExpect(status().isUnauthorized());
    }
}
