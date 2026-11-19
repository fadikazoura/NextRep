package com.fadikazoura.nextrep.stats;

import com.fadikazoura.nextrep.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class StatsIntegrationTest {

    private static final String WORKOUTS = "/api/users/me/workouts";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void newUserHasNoRecords() throws Exception {
        String token = newUser();

        mockMvc.perform(get("/api/users/me/records").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void computesRecordsAcrossWorkouts() throws Exception {
        String token = newUser();
        long bench = exerciseId(token, "CHEST", "BARBELL");
        long pushUp = exerciseId(token, "CHEST", "BODYWEIGHT");

        long first = startWorkout(token);
        addSet(token, first, bench, "100", 10);
        addSet(token, first, bench, "100", 8);
        addSet(token, first, pushUp, "0", 20);
        complete(token, first);

        long second = startWorkout(token);
        addSet(token, second, bench, "120", 1);
        complete(token, second);

        mockMvc.perform(get("/api/users/me/records").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].exerciseName", contains("Barbell Bench Press", "Push-Up")))
                .andExpect(jsonPath("$[0].muscleGroup").value("CHEST"))
                .andExpect(jsonPath("$[0].heaviest.weightKg", closeTo(120.0, 0.001)))
                .andExpect(jsonPath("$[0].heaviest.reps").value(1))
                .andExpect(jsonPath("$[0].heaviest.date").value(today()))
                .andExpect(jsonPath("$[0].bestEstimated.weightKg", closeTo(100.0, 0.001)))
                .andExpect(jsonPath("$[0].bestEstimated.reps").value(10))
                .andExpect(jsonPath("$[0].estimatedOneRepMaxKg", closeTo(133.33, 0.001)))
                .andExpect(jsonPath("$[1].heaviest", nullValue()))
                .andExpect(jsonPath("$[1].estimatedOneRepMaxKg", nullValue()))
                .andExpect(jsonPath("$[1].mostReps.reps").value(20));
    }

    @Test
    void ignoresWorkoutsThatAreNotCompleted() throws Exception {
        String token = newUser();
        long bench = exerciseId(token, "CHEST", "BARBELL");
        long workoutId = startWorkout(token);
        addSet(token, workoutId, bench, "200", 1);

        mockMvc.perform(get("/api/users/me/records").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/users/me/exercises/" + bench + "/progress").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.points", hasSize(0)));
    }

    @Test
    void progressSummarisesEachTrainingDay() throws Exception {
        String token = newUser();
        long bench = exerciseId(token, "CHEST", "BARBELL");
        long workoutId = startWorkout(token);
        addSet(token, workoutId, bench, "60", 10);
        addSet(token, workoutId, bench, "80", 10);
        addSet(token, workoutId, bench, "80", 8);
        complete(token, workoutId);

        mockMvc.perform(get("/api/users/me/exercises/" + bench + "/progress").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exerciseName").value("Barbell Bench Press"))
                .andExpect(jsonPath("$.points", hasSize(1)))
                .andExpect(jsonPath("$.points[0].date").value(today()))
                .andExpect(jsonPath("$.points[0].topWeightKg", closeTo(80.0, 0.001)))
                .andExpect(jsonPath("$.points[0].estimatedOneRepMaxKg", closeTo(106.67, 0.001)))
                .andExpect(jsonPath("$.points[0].volumeKg", closeTo(2040.0, 0.001)))
                .andExpect(jsonPath("$.points[0].maxReps").value(10));
    }

    @Test
    void progressOfUntrainedExerciseIsEmpty() throws Exception {
        String token = newUser();
        long squat = exerciseId(token, "QUADS", "BARBELL");

        mockMvc.perform(get("/api/users/me/exercises/" + squat + "/progress").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points", hasSize(0)));
    }

    @Test
    void progressOfUnknownExerciseIsNotFound() throws Exception {
        String token = newUser();

        mockMvc.perform(get("/api/users/me/exercises/999999/progress").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Exercise not found"));
    }

    @Test
    void doesNotIncludeOtherUsersData() throws Exception {
        String alice = newUser();
        long bench = exerciseId(alice, "CHEST", "BARBELL");
        long workoutId = startWorkout(alice);
        addSet(alice, workoutId, bench, "100", 5);
        complete(alice, workoutId);
        String bob = newUser();

        mockMvc.perform(get("/api/users/me/records").header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/users/me/exercises/" + bench + "/progress").header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$.points", hasSize(0)));
    }

    @Test
    void countsCompletedSetsPerMuscleGroup() throws Exception {
        String token = newUser();
        long bench = exerciseId(token, "CHEST", "BARBELL");
        long squat = exerciseId(token, "QUADS", "BARBELL");

        long done = startWorkout(token);
        addSet(token, done, bench, "60", 10);
        addSet(token, done, bench, "60", 10);
        addSet(token, done, squat, "80", 5);
        complete(token, done);

        long open = startWorkout(token); // not completed: must not count
        addSet(token, open, squat, "80", 5);

        mockMvc.perform(get("/api/users/me/muscles/volume").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[?(@.muscleGroup == 'CHEST')].sets").value(org.hamcrest.Matchers.contains(2)))
                .andExpect(jsonPath("$[?(@.muscleGroup == 'QUADS')].sets").value(org.hamcrest.Matchers.contains(1)))
                .andExpect(jsonPath("$[?(@.muscleGroup == 'BACK')].sets").value(org.hamcrest.Matchers.contains(0)));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me/records"))
                .andExpect(status().isUnauthorized());
    }

    private static String today() {
        return LocalDate.now(ZoneOffset.UTC).toString();
    }

    private long startWorkout(String token) throws Exception {
        String response = mockMvc.perform(post(WORKOUTS).header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private void addSet(String token, long workoutId, long exerciseId, String weightKg, int reps) throws Exception {
        mockMvc.perform(post(WORKOUTS + "/" + workoutId + "/sets")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exerciseId": %d, "weightKg": %s, "reps": %d}
                                """.formatted(exerciseId, weightKg, reps)))
                .andExpect(status().isCreated());
    }

    private void complete(String token, long workoutId) throws Exception {
        mockMvc.perform(post(WORKOUTS + "/" + workoutId + "/complete").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    /** First catalog exercise (by name) for a muscle group and equipment. */
    private long exerciseId(String token, String muscleGroup, String equipment) throws Exception {
        String response = mockMvc.perform(get("/api/exercises")
                        .param("muscleGroup", muscleGroup)
                        .param("equipment", equipment)
                        .header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$[0].id")).longValue();
    }

    private String newUser() throws Exception {
        String credentials = """
                {"email": "stats-%s@example.com", "password": "supersecret123"}
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
