package com.fadikazoura.nextrep.bodyweight;

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

import static org.hamcrest.Matchers.closeTo;
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
class BodyWeightIntegrationTest {

    private static final String PASSWORD = "supersecret123";
    private static final String URL = "/api/users/me/body-weight";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void logsWeightForTodayWhenDateIsOmitted() throws Exception {
        String token = registerAndLogin("bw-today@example.com");

        putWeight(token, "{\"weightKg\": 82.5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weightKg").value(82.5))
                .andExpect(jsonPath("$.recordedOn").value(LocalDate.now().toString()));
    }

    @Test
    void updatesSameDayInsteadOfCreatingDuplicate() throws Exception {
        String token = registerAndLogin("bw-update@example.com");

        String first = putWeight(token, "{\"weightKg\": 82.5}")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(first, "$.id");

        putWeight(token, "{\"weightKg\": 82.1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.weightKg").value(82.1));

        getHistory(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].weightKg", closeTo(82.1, 0.001)));
    }

    @Test
    void returnsHistoryOldestFirst() throws Exception {
        String token = registerAndLogin("bw-history@example.com");
        String recent = LocalDate.now().minusDays(3).toString();
        String older = LocalDate.now().minusDays(8).toString();

        putWeight(token, weightOn(81.2, recent)).andExpect(status().isOk());
        putWeight(token, weightOn(83.4, older)).andExpect(status().isOk());

        getHistory(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].recordedOn", contains(older, recent)));
    }

    @Test
    void rejectsOutOfRangeWeight() throws Exception {
        String token = registerAndLogin("bw-range@example.com");

        putWeight(token, "{\"weightKg\": 10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.weightKg").exists());
    }

    @Test
    void rejectsFutureDate() throws Exception {
        String token = registerAndLogin("bw-future@example.com");

        putWeight(token, weightOn(80.0, LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.recordedOn").exists());
    }

    @Test
    void doesNotShowOtherUsersEntries() throws Exception {
        String alice = registerAndLogin("bw-alice@example.com");
        String bob = registerAndLogin("bw-bob@example.com");

        putWeight(alice, "{\"weightKg\": 60.5}").andExpect(status().isOk());

        getHistory(bob)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    private static String weightOn(double weightKg, String date) {
        return "{\"weightKg\": %s, \"recordedOn\": \"%s\"}".formatted(weightKg, date);
    }

    private ResultActions putWeight(String token, String body) throws Exception {
        return mockMvc.perform(put(URL)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions getHistory(String token) throws Exception {
        return mockMvc.perform(get(URL).header("Authorization", "Bearer " + token));
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
