package com.fadikazoura.nextrep.exercise;

import com.fadikazoura.nextrep.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ExerciseIntegrationTest {

    private static final String URL = "/api/exercises";

    @Autowired
    private MockMvc mockMvc;

    private String token;

    @BeforeEach
    void login() throws Exception {
        token = registerAndLogin("exercise-" + UUID.randomUUID() + "@example.com");
    }

    @Test
    void returnsWholeCatalogWithoutFilters() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(58)));
    }

    @Test
    void filtersByMuscleGroup() throws Exception {
        mockMvc.perform(get(URL).param("muscleGroup", "CHEST").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(9)))
                .andExpect(jsonPath("$[*].muscleGroup", everyItem(is("CHEST"))));
    }

    @Test
    void filtersByMuscleGroupAndEquipmentSortedByName() throws Exception {
        mockMvc.perform(get(URL)
                        .param("muscleGroup", "CHEST")
                        .param("equipment", "DUMBBELL")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name",
                        contains("Dumbbell Bench Press", "Dumbbell Fly", "Incline Dumbbell Press")));
    }

    @Test
    void returnsSingleExerciseById() throws Exception {
        String list = mockMvc.perform(get(URL).param("muscleGroup", "QUADS").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(list, "$[0].id");
        String name = JsonPath.read(list, "$[0].name");

        mockMvc.perform(get(URL + "/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.muscleGroup").value("QUADS"));
    }

    @Test
    void returnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get(URL + "/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Exercise not found"));
    }

    @Test
    void rejectsUnknownFilterValue() throws Exception {
        mockMvc.perform(get(URL).param("muscleGroup", "WINGS").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid parameter"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
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
