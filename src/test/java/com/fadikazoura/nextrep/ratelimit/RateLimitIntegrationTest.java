package com.fadikazoura.nextrep.ratelimit;

import com.fadikazoura.nextrep.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rate limiting is off in the other tests; this context turns it on. Every test uses its own fake visitor IP. */
@SpringBootTest(properties = "nextrep.rate-limit.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void theEleventhLoginInAMinuteIsRejected() throws Exception {
        String ip = newIp();
        for (int i = 0; i < 10; i++) {
            login(ip).andExpect(status().isUnauthorized()); // wrong password, but still allowed to try
        }

        login(ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.title").value("Too many requests"))
                .andExpect(jsonPath("$.detail", containsString("try again")));
    }

    @Test
    void otherVisitorsAreNotAffected() throws Exception {
        String busy = newIp();
        for (int i = 0; i < 11; i++) login(busy);

        login(newIp()).andExpect(status().isUnauthorized());
    }

    @Test
    void demoAccountsAreLimitedPerVisitor() throws Exception {
        String ip = newIp();
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/demo").header("True-Client-IP", ip)).andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/auth/demo").header("True-Client-IP", ip)).andExpect(status().isTooManyRequests());
    }

    @Test
    void ordinaryEndpointsAreNotLimited() throws Exception {
        String ip = newIp();
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(get("/actuator/health").header("True-Client-IP", ip)).andExpect(status().isOk());
        }
    }

    private ResultActions login(String ip) throws Exception {
        return mockMvc.perform(post("/api/auth/login").header("True-Client-IP", ip)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"nobody@example.com\",\"password\":\"wrong-password\"}"));
    }

    /** The header only has to be unique per visitor; a real deployment gets an IP address in it. */
    private static String newIp() {
        return "test-" + UUID.randomUUID();
    }
}
