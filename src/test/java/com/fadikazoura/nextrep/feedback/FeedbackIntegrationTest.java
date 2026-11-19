package com.fadikazoura.nextrep.feedback;

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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FeedbackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ratingIsSavedAndCanBeChanged() throws Exception {
        String token = newUser();
        mockMvc.perform(get("/api/users/me/rating").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());

        rate(token, 4, "Great app").andExpect(status().isOk()).andExpect(jsonPath("$.stars").value(4));
        rate(token, 5, "").andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/rating").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stars").value(5))
                .andExpect(jsonPath("$.comment").doesNotExist());
    }

    @Test
    void starsMustBeBetweenOneAndFive() throws Exception {
        String token = newUser();
        rate(token, 0, null).andExpect(status().isBadRequest());
        rate(token, 6, null).andExpect(status().isBadRequest());
    }

    @Test
    void contactMessagesAreSavedAndLimitedPerDay() throws Exception {
        String token = newUser();
        for (int i = 0; i < FeedbackService.MESSAGES_PER_DAY; i++) {
            contact(token, "BUG", "The rest timer does not beep on my phone #" + i).andExpect(status().isCreated());
        }
        contact(token, "IDEA", "One more message that is over the daily limit")
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(get("/api/users/me/export").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.contactMessages", hasSize(FeedbackService.MESSAGES_PER_DAY)));
    }

    @Test
    void contactMessageNeedsATopicAndARealMessage() throws Exception {
        String token = newUser();
        contact(token, "BUG", "short").andExpect(status().isBadRequest());
        contact(token, "SPAM", "A message with a topic that does not exist").andExpect(status().isBadRequest());
    }

    private ResultActions rate(String token, int stars, String comment) throws Exception {
        String commentJson = comment == null ? "null" : "\"" + comment + "\"";
        return mockMvc.perform(put("/api/users/me/rating").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"stars": %d, "comment": %s}
                        """.formatted(stars, commentJson)));
    }

    private ResultActions contact(String token, String topic, String message) throws Exception {
        return mockMvc.perform(post("/api/users/me/contact").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"topic": "%s", "message": "%s"}
                        """.formatted(topic, message)));
    }

    private String newUser() throws Exception {
        String email = "fb-" + UUID.randomUUID() + "@example.com";
        String credentials = """
                {"email": "%s", "password": "supersecret123"}
                """.formatted(email);
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isCreated());
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
