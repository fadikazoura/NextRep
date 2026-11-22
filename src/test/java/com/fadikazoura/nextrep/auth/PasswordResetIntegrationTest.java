package com.fadikazoura.nextrep.auth;

import com.fadikazoura.nextrep.TestcontainersConfiguration;
import com.fadikazoura.nextrep.mail.MailMessage;
import com.fadikazoura.nextrep.mail.MailSender;
import com.jayway.jsonpath.JsonPath;
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

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PasswordResetIntegrationTest {

    private static final String PASSWORD = "supersecret123";
    private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MailSender mailSender;

    @Test
    void unknownEmailGetsTheSameAnswerButNoEmail() throws Exception {
        requestCode("nobody-" + UUID.randomUUID() + "@example.com").andExpect(status().isAccepted());

        verify(mailSender, never()).send(any());
    }

    @Test
    void resetsThePasswordWithTheEmailedCode() throws Exception {
        String email = newUser();
        String oldToken = token(email, PASSWORD);

        requestCode(email).andExpect(status().isAccepted());
        String code = lastEmailedCode(email);

        confirm(email, wrongCode(code), "brandnewpass1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.code").exists());
        confirm(email, code, "brandnewpass1").andExpect(status().isNoContent());

        login(email, PASSWORD).andExpect(status().isUnauthorized());
        login(email, "brandnewpass1").andExpect(status().isOk());
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized()); // every device was logged out

        confirm(email, code, "anotherpass22").andExpect(status().isBadRequest()); // a code works only once
    }

    @Test
    void codeIsBlockedAfterFiveWrongGuesses() throws Exception {
        String email = newUser();
        requestCode(email);
        String code = lastEmailedCode(email);

        for (int i = 0; i < PasswordResetCode.MAX_ATTEMPTS; i++) {
            confirm(email, wrongCode(code), "brandnewpass1").andExpect(status().isBadRequest());
        }
        confirm(email, code, "brandnewpass1").andExpect(status().isBadRequest());
        login(email, PASSWORD).andExpect(status().isOk()); // password unchanged
    }

    @Test
    void aNewCodeReplacesTheOldOne() throws Exception {
        String email = newUser();
        requestCode(email);
        String first = lastEmailedCode(email);
        requestCode(email);
        String second = lastEmailedCode(email);

        if (!first.equals(second)) {
            confirm(email, first, "brandnewpass1").andExpect(status().isBadRequest());
        }
        confirm(email, second, "brandnewpass1").andExpect(status().isNoContent());
    }

    @Test
    void atMostThreeCodesPerHour() throws Exception {
        String email = newUser();
        for (int i = 0; i < 4; i++) {
            requestCode(email).andExpect(status().isAccepted());
        }

        verify(mailSender, times(PasswordResetService.MAX_CODES_PER_HOUR)).send(any());
    }

    @Test
    void contactMessagesAreEmailedToTheTeamWithReplyTo() throws Exception {
        String email = newUser();
        String token = token(email, PASSWORD);

        mockMvc.perform(post("/api/users/me/contact").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topic": "BUG", "message": "The timer does not beep on my phone"}
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<MailMessage> mail = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailSender).send(mail.capture());
        assertThat(mail.getValue().to()).isEqualTo("nextrep.fitapp@gmail.com");
        assertThat(mail.getValue().replyTo()).isEqualTo(email);
        assertThat(mail.getValue().text()).contains("The timer does not beep on my phone");
    }

    /* ---------- helpers ---------- */

    private String lastEmailedCode(String email) {
        ArgumentCaptor<MailMessage> mail = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailSender, atLeastOnce()).send(mail.capture());
        MailMessage last = mail.getValue();
        assertThat(last.to()).isEqualTo(email);
        Matcher matcher = CODE.matcher(last.text());
        assertThat(matcher.find()).isTrue();
        clearInvocations(mailSender);
        return matcher.group(1);
    }

    private static String wrongCode(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }

    private ResultActions requestCode(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/password-reset").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s"}
                        """.formatted(email)));
    }

    private ResultActions confirm(String email, String code, String newPassword) throws Exception {
        return mockMvc.perform(post("/api/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "code": "%s", "newPassword": "%s"}
                        """.formatted(email, code, newPassword)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"login": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }

    private String token(String email, String password) throws Exception {
        return JsonPath.read(login(email, password).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    private String newUser() throws Exception {
        String email = "reset-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
        return email;
    }
}
