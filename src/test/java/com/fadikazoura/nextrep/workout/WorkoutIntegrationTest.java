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
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WorkoutIntegrationTest {

    private static final String URL = "/api/users/me/workouts";

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
              "trainingDays": ["MONDAY", "WEDNESDAY", "FRIDAY"]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void logsAFullWorkout() throws Exception {
        String token = newUser();
        long bench = exerciseId(token, "CHEST");
        long workoutId = startWorkout(token);

        addSet(token, workoutId, bench, "80.5", 10)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.setNumber").value(1))
                .andExpect(jsonPath("$.weightKg", closeTo(80.5, 0.001)))
                .andExpect(jsonPath("$.reps").value(10));
        addSet(token, workoutId, bench, "80.5", 8).andExpect(status().isCreated());

        mockMvc.perform(post(URL + "/" + workoutId + "/complete").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt", notNullValue()));

        mockMvc.perform(get(URL + "/" + workoutId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sets", hasSize(2)))
                .andExpect(jsonPath("$.sets[*].reps", contains(10, 8)));
    }

    @Test
    void countsSetNumbersPerExercise() throws Exception {
        String token = newUser();
        long bench = exerciseId(token, "CHEST");
        long squat = exerciseId(token, "QUADS");
        long workoutId = startWorkout(token);

        addSet(token, workoutId, bench, "60", 10);
        addSet(token, workoutId, bench, "60", 10);
        addSet(token, workoutId, squat, "100", 5)
                .andExpect(jsonPath("$.setNumber").value(1));

        mockMvc.perform(get(URL + "/" + workoutId).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.sets[*].setNumber", contains(1, 2, 1)));
    }

    @Test
    void allowsOnlyOneWorkoutInProgress() throws Exception {
        String token = newUser();
        startWorkout(token);

        mockMvc.perform(post(URL).header("Authorization", bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Workout in progress"));
    }

    @Test
    void currentReturnsOpenWorkoutUntilItIsCompleted() throws Exception {
        String token = newUser();
        long workoutId = startWorkout(token);

        mockMvc.perform(get(URL + "/current").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workoutId))
                .andExpect(jsonPath("$.completedAt", nullValue()));

        addSet(token, workoutId, exerciseId(token, "BACK"), "50", 12);
        mockMvc.perform(post(URL + "/" + workoutId + "/complete").header("Authorization", bearer(token)));

        mockMvc.perform(get(URL + "/current").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No workout in progress"));
    }

    @Test
    void completedWorkoutCannotBeChanged() throws Exception {
        String token = newUser();
        long bench = exerciseId(token, "CHEST");
        long workoutId = startWorkout(token);
        addSet(token, workoutId, bench, "70", 10);
        mockMvc.perform(post(URL + "/" + workoutId + "/complete").header("Authorization", bearer(token)));

        addSet(token, workoutId, bench, "70", 10)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Workout already completed"));
        mockMvc.perform(delete(URL + "/" + workoutId).header("Authorization", bearer(token)))
                .andExpect(status().isConflict());
    }

    @Test
    void emptyWorkoutCannotBeCompleted() throws Exception {
        String token = newUser();
        long workoutId = startWorkout(token);

        mockMvc.perform(post(URL + "/" + workoutId + "/complete").header("Authorization", bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Empty workout"));
    }

    @Test
    void deletingASetKeepsNumberingGoing() throws Exception {
        String token = newUser();
        long bench = exerciseId(token, "CHEST");
        long workoutId = startWorkout(token);
        addSet(token, workoutId, bench, "60", 10);
        Integer secondSetId = JsonPath.read(addSet(token, workoutId, bench, "60", 10)
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(delete(URL + "/" + workoutId + "/sets/" + secondSetId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(URL + "/" + workoutId).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.sets", hasSize(1)));
        addSet(token, workoutId, bench, "60", 9)
                .andExpect(jsonPath("$.setNumber").value(2));
    }

    @Test
    void deletingUnknownSetReturnsNotFound() throws Exception {
        String token = newUser();
        long workoutId = startWorkout(token);

        mockMvc.perform(delete(URL + "/" + workoutId + "/sets/999999").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Set not found"));
    }

    @Test
    void discardsWorkoutInProgress() throws Exception {
        String token = newUser();
        long workoutId = startWorkout(token);

        mockMvc.perform(delete(URL + "/" + workoutId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(URL + "/" + workoutId).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(URL).header("Authorization", bearer(token)))
                .andExpect(status().isCreated());
    }

    @Test
    void startsWorkoutFromOwnPlanDay() throws Exception {
        String token = newUser();
        long planDayId = firstPlanDayId(token);

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planDayId\": " + planDayId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planDayId").value(planDayId))
                .andExpect(jsonPath("$.planDayName").value("Full Body"));
    }

    @Test
    void cannotStartWorkoutFromAnotherUsersPlanDay() throws Exception {
        long alicePlanDayId = firstPlanDayId(newUser());
        String bob = newUser();

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planDayId\": " + alicePlanDayId + "}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Plan day not found"));
    }

    @Test
    void otherUsersCannotSeeOrChangeAWorkout() throws Exception {
        String alice = newUser();
        long workoutId = startWorkout(alice);
        String bob = newUser();

        mockMvc.perform(get(URL + "/" + workoutId).header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Workout not found"));
        addSet(bob, workoutId, exerciseId(bob, "CHEST"), "60", 10)
                .andExpect(status().isNotFound());
    }

    @Test
    void validatesSetValues() throws Exception {
        String token = newUser();
        long workoutId = startWorkout(token);

        addSet(token, workoutId, exerciseId(token, "CHEST"), "-5", 150)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.weightKg").exists())
                .andExpect(jsonPath("$.errors.reps").exists());
    }

    @Test
    void rejectsUnknownExercise() throws Exception {
        String token = newUser();
        long workoutId = startWorkout(token);

        addSet(token, workoutId, 999999L, "60", 10)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Exercise not found"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post(URL))
                .andExpect(status().isUnauthorized());
    }

    private long startWorkout(String token) throws Exception {
        String response = mockMvc.perform(post(URL).header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private ResultActions addSet(String token, long workoutId, long exerciseId, String weightKg, int reps)
            throws Exception {
        return mockMvc.perform(post(URL + "/" + workoutId + "/sets")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"exerciseId": %d, "weightKg": %s, "reps": %d}
                        """.formatted(exerciseId, weightKg, reps)));
    }

    private long exerciseId(String token, String muscleGroup) throws Exception {
        String response = mockMvc.perform(get("/api/exercises")
                        .param("muscleGroup", muscleGroup)
                        .header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$[0].id")).longValue();
    }

    private long firstPlanDayId(String token) throws Exception {
        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROFILE))
                .andExpect(status().isOk());
        String plan = mockMvc.perform(post("/api/users/me/plan/generate").header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(plan, "$.days[0].id")).longValue();
    }

    private String newUser() throws Exception {
        String credentials = """
                {"email": "workout-%s@example.com", "password": "supersecret123"}
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
