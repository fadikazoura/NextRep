package com.fadikazoura.nextrep.account;

import com.fadikazoura.nextrep.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountIntegrationTest {

    private static final String PASSWORD = "supersecret123";
    private static final byte[] TINY_PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    @Autowired
    private MockMvc mockMvc;

    /* ---------- registration and login ---------- */

    @Test
    void registersWithUsernameAndPhoneAndLogsInWithEitherOne() throws Exception {
        String username = uniqueUsername();
        String email = username + "@example.com";
        register(username, email, "+972 52-555-1234")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.phone").value("+972525551234"))
                .andExpect(jsonPath("$.avatar.kind").value("INITIALS"));

        login(username.toUpperCase()).andExpect(status().isOk()); // username ignores case
        login(email).andExpect(status().isOk());
    }

    @Test
    void usernameMustBeUniqueIgnoringCase() throws Exception {
        String username = uniqueUsername();
        register(username, username + "@example.com", null).andExpect(status().isCreated());

        register(username.toUpperCase(), "other-" + username + "@example.com", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    void rejectsInvalidUsernameAndPhone() throws Exception {
        register("no spaces!", uniqueUsername() + "@example.com", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists());

        String username = uniqueUsername();
        register(username, username + "@example.com", "+972 12")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").exists());
    }

    @Test
    void apiClientsWithoutAUsernameGetOneFromTheirEmail() throws Exception {
        String email = "Auto." + UUID.randomUUID().toString().substring(0, 6) + "@example.com";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(email.substring(0, email.indexOf('@')).toLowerCase()));
    }

    /* ---------- account settings ---------- */

    @Test
    void updatesUsernameAndRemovesPhone() throws Exception {
        String token = newUser();
        String newName = uniqueUsername();

        mockMvc.perform(put("/api/users/me/account").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "phone": ""}
                                """.formatted(newName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(newName))
                .andExpect(jsonPath("$.phone").doesNotExist());
    }

    @Test
    void changingThePasswordNeedsTheCurrentOneAndLogsOutOtherDevices() throws Exception {
        String username = uniqueUsername();
        register(username, username + "@example.com", null).andExpect(status().isCreated());
        String oldToken = token(username, PASSWORD);

        changePassword(oldToken, "wrong-password", "brandnewpass1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.currentPassword").exists());

        String newToken = JsonPath.read(changePassword(oldToken, PASSWORD, "brandnewpass1")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.accessToken");

        me(oldToken).andExpect(status().isUnauthorized()); // the old token was revoked
        me(newToken).andExpect(status().isOk());
        login(username).andExpect(status().isUnauthorized()); // old password no longer works
    }

    @Test
    void logOutEverywhereRevokesEveryToken() throws Exception {
        String token = newUser();

        mockMvc.perform(post("/api/users/me/sessions/revoke").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        me(token).andExpect(status().isUnauthorized());
    }

    @Test
    void exportsAllUserData() throws Exception {
        String token = newUser();

        mockMvc.perform(get("/api/users/me/export").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(jsonPath("$.account.username").exists())
                .andExpect(jsonPath("$.workouts").isArray())
                .andExpect(jsonPath("$.plans").isArray());
    }

    @Test
    void deletingTheAccountNeedsThePasswordAndRemovesTheUser() throws Exception {
        String token = newUser();

        deleteAccount(token, "wrong-password").andExpect(status().isBadRequest());
        deleteAccount(token, PASSWORD).andExpect(status().isNoContent());

        me(token).andExpect(status().isUnauthorized());
    }

    /* ---------- avatar ---------- */

    @Test
    void choosesAPresetAvatarButRejectsUnknownOnes() throws Exception {
        String token = newUser();

        setPreset(token, "kettlebell")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar.kind").value("PRESET"))
                .andExpect(jsonPath("$.avatar.preset").value("kettlebell"));

        setPreset(token, "../../etc/passwd").andExpect(status().isBadRequest());
    }

    @Test
    void uploadsAPhotoAndServesItBack() throws Exception {
        String token = newUser();

        mockMvc.perform(multipart("/api/users/me/avatar/photo")
                        .file(new MockMultipartFile("file", "me.png", "image/png", TINY_PNG))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar.kind").value("PHOTO"));

        mockMvc.perform(get("/api/users/me/avatar/photo").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(TINY_PNG));

        mockMvc.perform(delete("/api/users/me/avatar").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar.kind").value("INITIALS"));
    }

    @Test
    void rejectsFilesThatAreNotPictures() throws Exception {
        String token = newUser();
        byte[] script = "<script>alert(1)</script>".getBytes();

        mockMvc.perform(multipart("/api/users/me/avatar/photo")
                        .file(new MockMultipartFile("file", "evil.png", "image/png", script))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    /* ---------- helpers ---------- */

    private ResultActions register(String username, String email, String phone) throws Exception {
        String phoneJson = phone == null ? "null" : "\"" + phone + "\"";
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "%s", "email": "%s", "password": "%s", "phone": %s}
                        """.formatted(username, email, PASSWORD, phoneJson)));
    }

    private ResultActions login(String login) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"login": "%s", "password": "%s"}
                        """.formatted(login, PASSWORD)));
    }

    private String token(String login, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login": "%s", "password": "%s"}
                                """.formatted(login, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private String newUser() throws Exception {
        String username = uniqueUsername();
        register(username, username + "@example.com", null).andExpect(status().isCreated());
        return token(username, PASSWORD);
    }

    private ResultActions me(String token) throws Exception {
        return mockMvc.perform(get("/api/users/me").header("Authorization", bearer(token)));
    }

    private ResultActions changePassword(String token, String current, String next) throws Exception {
        return mockMvc.perform(put("/api/users/me/password").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword": "%s", "newPassword": "%s"}
                        """.formatted(current, next)));
    }

    private ResultActions deleteAccount(String token, String password) throws Exception {
        return mockMvc.perform(delete("/api/users/me").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"password": "%s"}
                        """.formatted(password)));
    }

    private ResultActions setPreset(String token, String preset) throws Exception {
        return mockMvc.perform(put("/api/users/me/avatar").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"preset": "%s"}
                        """.formatted(preset)));
    }

    private static String uniqueUsername() {
        return "u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
