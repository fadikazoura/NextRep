package com.fadikazoura.nextrep.demo;

import com.fadikazoura.nextrep.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DemoAccountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DemoAccountService demoAccountService;

    @Test
    void createsAReadyToUseAccountWithHistory() throws Exception {
        String token = demoToken();

        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demo").value(true))
                .andExpect(jsonPath("$.username").value(startsWith("demo_")));
        mockMvc.perform(get("/api/users/me/profile").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/me/plan").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days", not(hasSize(0))));
        mockMvc.perform(get("/api/users/me/workouts?size=5").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThan(15)));
        mockMvc.perform(get("/api/users/me/records").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(hasSize(0))));
        mockMvc.perform(get("/api/users/me/body-weight").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(hasSize(0))));
    }

    @Test
    void everyVisitorGetsTheirOwnAccount() throws Exception {
        String first = username(demoToken());
        String second = username(demoToken());

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void normalAccountsAreNotDemo() throws Exception {
        String email = "real-" + System.nanoTime() + "@example.com";
        mockMvc.perform(post("/api/auth/register").contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"supersecret123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.demo").value(false));
    }

    @Test
    void expiredDemoAccountsAreDeletedWithTheirData() throws Exception {
        String token = demoToken();
        long userId = userId(token);
        jdbcTemplate.update("UPDATE users SET demo_expires_at = now() - interval '1 minute' WHERE id = ?", userId);

        assertThat(demoAccountService.deleteExpiredAccounts()).isPositive();

        assertThat(count("SELECT count(*) FROM users WHERE id = ?", userId)).isZero();
        assertThat(count("SELECT count(*) FROM workout_sessions WHERE user_id = ?", userId)).isZero();
        assertThat(count("SELECT count(*) FROM body_weight_entries WHERE user_id = ?", userId)).isZero();
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void unexpiredDemoAccountsAreKept() throws Exception {
        long userId = userId(demoToken());

        demoAccountService.deleteExpiredAccounts();

        assertThat(count("SELECT count(*) FROM users WHERE id = ?", userId)).isEqualTo(1);
    }

    private String demoToken() throws Exception {
        String body = mockMvc.perform(post("/api/auth/demo"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String username(String token) throws Exception {
        return JsonPath.read(me(token), "$.username");
    }

    private long userId(String token) throws Exception {
        return ((Number) JsonPath.read(me(token), "$.id")).longValue();
    }

    private String me(String token) throws Exception {
        return mockMvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString();
    }

    private long count(String sql, long userId) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class, userId);
        return result == null ? 0 : result;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
