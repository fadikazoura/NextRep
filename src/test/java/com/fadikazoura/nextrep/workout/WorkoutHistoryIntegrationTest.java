package com.fadikazoura.nextrep.workout;

import com.fadikazoura.nextrep.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WorkoutHistoryIntegrationTest {

    private static final String URL = "/api/users/me/workouts";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsCompletedWorkoutsNewestFirstWithTotals() throws Exception {
        String token = newUser();
        long bench = exerciseId(token, "CHEST");
        long row = exerciseId(token, "BACK");

        long first = startWorkout(token);
        addSet(token, first, bench, "100", 5);
        addSet(token, first, bench, "100", 5);
        complete(token, first);

        long second = startWorkout(token);
        addSet(token, second, bench, "60", 10);
        addSet(token, second, row, "50.5", 10);
        complete(token, second);

        startWorkout(token); // still in progress, must not appear in the history

        mockMvc.perform(get(URL).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(second))
                .andExpect(jsonPath("$.content[0].setCount").value(2))
                .andExpect(jsonPath("$.content[0].exerciseCount").value(2))
                .andExpect(jsonPath("$.content[0].totalVolumeKg", closeTo(1105.0, 0.001)))
                .andExpect(jsonPath("$.content[1].id").value(first))
                .andExpect(jsonPath("$.content[1].exerciseCount").value(1))
                .andExpect(jsonPath("$.content[1].totalVolumeKg", closeTo(1000.0, 0.001)));
    }

    @Test
    void paginatesHistory() throws Exception {
        String token = newUser();
        long bench = exerciseId(token, "CHEST");
        for (int i = 0; i < 3; i++) {
            long workoutId = startWorkout(token);
            addSet(token, workoutId, bench, "60", 10);
            complete(token, workoutId);
        }

        mockMvc.perform(get(URL).param("size", "2").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get(URL).param("size", "2").param("page", "1").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void capsPageSize() throws Exception {
        String token = newUser();

        mockMvc.perform(get(URL).param("size", "5000").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(WorkoutService.MAX_PAGE_SIZE));
    }

    @Test
    void showsOnlyOwnWorkouts() throws Exception {
        String alice = newUser();
        long workoutId = startWorkout(alice);
        addSet(alice, workoutId, exerciseId(alice, "CHEST"), "60", 10);
        complete(alice, workoutId);
        String bob = newUser();

        mockMvc.perform(get(URL).header("Authorization", bearer(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private long startWorkout(String token) throws Exception {
        String response = mockMvc.perform(post(URL).header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private void addSet(String token, long workoutId, long exerciseId, String weightKg, int reps) throws Exception {
        mockMvc.perform(post(URL + "/" + workoutId + "/sets")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exerciseId": %d, "weightKg": %s, "reps": %d}
                                """.formatted(exerciseId, weightKg, reps)))
                .andExpect(status().isCreated());
    }

    private void complete(String token, long workoutId) throws Exception {
        mockMvc.perform(post(URL + "/" + workoutId + "/complete").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    private long exerciseId(String token, String muscleGroup) throws Exception {
        String response = mockMvc.perform(get("/api/exercises")
                        .param("muscleGroup", muscleGroup)
                        .header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$[0].id")).longValue();
    }

    private String newUser() throws Exception {
        String credentials = """
                {"email": "history-%s@example.com", "password": "supersecret123"}
                """.formatted(UUID.randomUUID());

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

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
