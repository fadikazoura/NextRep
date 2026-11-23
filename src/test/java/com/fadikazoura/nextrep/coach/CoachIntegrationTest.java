package com.fadikazoura.nextrep.coach;

import com.fadikazoura.nextrep.TestcontainersConfiguration;
import com.fadikazoura.nextrep.ai.AiUnavailableException;
import com.fadikazoura.nextrep.ai.GeminiClient;
import com.fadikazoura.nextrep.ai.GeminiClient.ChatTurn;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Gemini is a Mockito bean here: the tests check what the coach sends and how it answers, without the real API. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CoachIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeminiClient geminiClient;

    @BeforeEach
    void aiIsAvailable() {
        when(geminiClient.isEnabled()).thenReturn(true);
        when(geminiClient.generateText(anyString(), anyList(), anyDouble(), anyInt())).thenReturn("  Add 2.5 kg next time.  ");
    }

    @Test
    @SuppressWarnings("unchecked")
    void answersWithTheAthletesDataAsContext() throws Exception {
        String token = demoToken();

        ask(token, "{\"question\":\"Why is my bench stuck?\",\"language\":\"en\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Add 2.5 kg next time."))
                .andExpect(jsonPath("$.remainingToday").value(CoachQuota.DAILY_QUESTIONS - 1));

        ArgumentCaptor<String> instruction = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<ChatTurn>> turns = ArgumentCaptor.forClass(List.class);
        verify(geminiClient).generateText(instruction.capture(), turns.capture(), anyDouble(), anyInt());
        assertThat(instruction.getValue())
                .contains("NextRep Coach", "Answer in English", "PROFILE:", "PLAN \"", "RECENT WORKOUTS", "PERSONAL RECORDS")
                .doesNotContain("@demo.nextrep.invalid"); // no email or other personal details are sent
        assertThat(turns.getValue()).containsExactly(new ChatTurn(true, "Why is my bench stuck?"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void keepsTheConversationAndAnswersInHebrew() throws Exception {
        String token = demoToken();

        ask(token, """
                {"question":"ומה עם סקוואט?","language":"he",
                 "history":[{"fromUser":true,"text":"מה לעשות היום?"},{"fromUser":false,"text":"Upper A"}]}
                """).andExpect(status().isOk());

        ArgumentCaptor<List<ChatTurn>> turns = ArgumentCaptor.forClass(List.class);
        verify(geminiClient).generateText(org.mockito.ArgumentMatchers.contains("Answer in Hebrew"), turns.capture(),
                anyDouble(), anyInt());
        assertThat(turns.getValue()).extracting(ChatTurn::fromUser).containsExactly(true, false, true);
    }

    @Test
    void theDailyAllowanceRunsOut() throws Exception {
        String token = demoToken();
        for (int i = 0; i < CoachQuota.DAILY_QUESTIONS; i++) {
            ask(token, "{\"question\":\"Question " + i + "\"}").andExpect(status().isOk());
        }

        ask(token, "{\"question\":\"One more\"}")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title").value("Daily limit reached"));
    }

    @Test
    void aFailedAiCallDoesNotCountAndSaysUnavailable() throws Exception {
        String token = demoToken();
        when(geminiClient.generateText(anyString(), anyList(), anyDouble(), anyInt()))
                .thenThrow(new AiUnavailableException("overloaded"));

        ask(token, "{\"question\":\"Hi\"}").andExpect(status().isServiceUnavailable());

        // doReturn, not when(...): when() would call the method, which is still set to throw
        doReturn("OK").when(geminiClient).generateText(anyString(), anyList(), anyDouble(), anyInt());
        ask(token, "{\"question\":\"Hi again\"}")
                .andExpect(jsonPath("$.remainingToday").value(CoachQuota.DAILY_QUESTIONS - 1));
    }

    @Test
    void withoutAnAiKeyTheCoachIsUnavailable() throws Exception {
        when(geminiClient.isEnabled()).thenReturn(false);

        ask(demoToken(), "{\"question\":\"Hi\"}").andExpect(status().isServiceUnavailable());
    }

    @Test
    void rejectsEmptyAndOverlongQuestions() throws Exception {
        String token = demoToken();

        ask(token, "{\"question\":\"  \"}").andExpect(status().isBadRequest());
        ask(token, "{\"question\":\"" + "a".repeat(501) + "\"}").andExpect(status().isBadRequest());
        ask(token, "{\"question\":\"Hi\",\"language\":\"fr\"}").andExpect(status().isBadRequest());
    }

    @Test
    void requiresAToken() throws Exception {
        mockMvc.perform(post("/api/users/me/coach").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"Hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions ask(String token, String json) throws Exception {
        return mockMvc.perform(post("/api/users/me/coach?tz=Asia/Jerusalem")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private String demoToken() throws Exception {
        String body = mockMvc.perform(post("/api/auth/demo")).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
