package com.fadikazoura.nextrep.auth;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LoginFlowIntegrationTest {

    private static final String PASSWORD = "supersecret123";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registeredUserCanLogInAndAccessProtectedEndpoint() throws Exception {
        String email = "flow@example.com";
        postJson("/api/auth/register", email, PASSWORD).andExpect(status().isCreated());

        String response = postJson("/api/auth/login", email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.accessToken");

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        postJson("/api/auth/register", "wrongpass@example.com", PASSWORD).andExpect(status().isCreated());

        postJson("/api/auth/login", "wrongpass@example.com", "not-the-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email/username or password"));
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsTamperedToken() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer not.a.real-token"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions postJson(String url, String email, String password) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }
}
