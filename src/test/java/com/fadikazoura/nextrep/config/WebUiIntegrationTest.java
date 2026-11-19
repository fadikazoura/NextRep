package com.fadikazoura.nextrep.config;

import com.fadikazoura.nextrep.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WebUiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesTheAppShellWithoutLogin() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>NextRep</title>")));
    }

    @Test
    void servesScriptsAndStylesWithoutLogin() throws Exception {
        mockMvc.perform(get("/js/app.js")).andExpect(status().isOk());
        mockMvc.perform(get("/css/app.css")).andExpect(status().isOk());
    }

    @Test
    void servesInstallableAppFilesWithoutLogin() throws Exception {
        mockMvc.perform(get("/manifest.json"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"short_name\": \"NextRep\"")));
        mockMvc.perform(get("/sw.js")).andExpect(status().isOk());
        mockMvc.perform(get("/icons/icon-192.png")).andExpect(status().isOk());
        mockMvc.perform(get("/fonts/barlow-condensed-700.woff2")).andExpect(status().isOk());
    }

    @Test
    void theApiStillRequiresAToken() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }
}
