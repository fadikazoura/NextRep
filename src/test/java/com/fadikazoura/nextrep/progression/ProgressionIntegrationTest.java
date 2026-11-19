package com.fadikazoura.nextrep.progression;

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
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProgressionIntegrationTest {

    private static final String WORKOUTS = "/api/users/me/workouts";

    /** 3 days, full gym, hypertrophy, intermediate: first exercise is a compound lift, 4 sets × 8–12. */
    private static final String PROFILE = """
            {
              "birthDate": "2001-07-20",
              "sex": "FEMALE",
              "heightCm": 168,
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
    void newUserGetsStartRecommendationsForEveryExerciseOfTheDay() throws Exception {
        User user = newUserWithPlan();

        recommendations(user)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planDayId").value(user.planDayId))
                .andExpect(jsonPath("$.dayName").value("Full Body"))
                .andExpect(jsonPath("$.exercises", hasSize(6)))
                .andExpect(jsonPath("$.exercises[0].exerciseId").value(user.firstExerciseId))
                .andExpect(jsonPath("$.exercises[*].action", everyItem(is("START"))))
                .andExpect(jsonPath("$.exercises[0].weightKg", nullValue()))
                .andExpect(jsonPath("$.exercises[0].sets").value(4))
                .andExpect(jsonPath("$.exercises[0].repMin").value(8))
                .andExpect(jsonPath("$.exercises[0].repMax").value(12));
    }

    @Test
    void increasesWeightAfterHittingTopOfRangeOnAllSets() throws Exception {
        User user = newUserWithPlan();
        completeWorkout(user, "100", 12, 12, 12, 12);

        recommendations(user)
                .andExpect(jsonPath("$.exercises[0].action").value("INCREASE"))
                .andExpect(jsonPath("$.exercises[0].weightKg", closeTo(102.5, 0.001)))
                .andExpect(jsonPath("$.exercises[0].reason").value("Hit 12 reps on all 4 sets at 100 kg last time"))
                .andExpect(jsonPath("$.exercises[1].action").value("START"));
    }

    @Test
    void holdsWeightAfterMissingReps() throws Exception {
        User user = newUserWithPlan();
        completeWorkout(user, "100", 12, 12, 10, 9);

        recommendations(user)
                .andExpect(jsonPath("$.exercises[0].action").value("HOLD"))
                .andExpect(jsonPath("$.exercises[0].weightKg", closeTo(100.0, 0.001)));
    }

    @Test
    void deloadsAfterThreeStalledWorkouts() throws Exception {
        User user = newUserWithPlan();
        for (int i = 0; i < 3; i++) {
            completeWorkout(user, "100", 9, 9, 9, 9);
        }

        recommendations(user)
                .andExpect(jsonPath("$.exercises[0].action").value("DELOAD"))
                .andExpect(jsonPath("$.exercises[0].weightKg", closeTo(90.0, 0.001)));
    }

    @Test
    void ignoresWorkoutsThatAreNotCompleted() throws Exception {
        User user = newUserWithPlan();
        long workoutId = startWorkout(user);
        for (int i = 0; i < 4; i++) {
            addSet(user, workoutId, "100", 12);
        }

        recommendations(user)
                .andExpect(jsonPath("$.exercises[0].action").value("START"));
    }

    @Test
    void doesNotUseAnotherUsersHistory() throws Exception {
        User alice = newUserWithPlan();
        completeWorkout(alice, "100", 12, 12, 12, 12);
        User bob = newUserWithPlan();

        recommendations(bob)
                .andExpect(jsonPath("$.exercises[*].action", everyItem(is("START"))));
    }

    @Test
    void rejectsAnotherUsersPlanDay() throws Exception {
        User alice = newUserWithPlan();
        User bob = newUserWithPlan();

        mockMvc.perform(get("/api/users/me/plan/days/" + alice.planDayId + "/recommendations")
                        .header("Authorization", bearer(bob.token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Plan day not found"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me/plan/days/1/recommendations"))
                .andExpect(status().isUnauthorized());
    }

    private record User(String token, long planDayId, long firstExerciseId) {
    }

    private ResultActions recommendations(User user) throws Exception {
        return mockMvc.perform(get("/api/users/me/plan/days/" + user.planDayId + "/recommendations")
                .header("Authorization", bearer(user.token)));
    }

    /** Logs one workout on the plan day with the given sets of the day's first exercise, then completes it. */
    private void completeWorkout(User user, String weightKg, int... reps) throws Exception {
        long workoutId = startWorkout(user);
        for (int rep : reps) {
            addSet(user, workoutId, weightKg, rep);
        }
        mockMvc.perform(post(WORKOUTS + "/" + workoutId + "/complete").header("Authorization", bearer(user.token)))
                .andExpect(status().isOk());
    }

    private long startWorkout(User user) throws Exception {
        String response = mockMvc.perform(post(WORKOUTS)
                        .header("Authorization", bearer(user.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planDayId\": " + user.planDayId + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private void addSet(User user, long workoutId, String weightKg, int reps) throws Exception {
        mockMvc.perform(post(WORKOUTS + "/" + workoutId + "/sets")
                        .header("Authorization", bearer(user.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exerciseId": %d, "weightKg": %s, "reps": %d}
                                """.formatted(user.firstExerciseId, weightKg, reps)))
                .andExpect(status().isCreated());
    }

    private User newUserWithPlan() throws Exception {
        String credentials = """
                {"email": "progression-%s@example.com", "password": "supersecret123"}
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
        String plan = mockMvc.perform(post("/api/users/me/plan/generate").header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long planDayId = ((Number) JsonPath.read(plan, "$.days[0].id")).longValue();
        long firstExerciseId = ((Number) JsonPath.read(plan, "$.days[0].exercises[0].exerciseId")).longValue();
        return new User(token, planDayId, firstExerciseId);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
