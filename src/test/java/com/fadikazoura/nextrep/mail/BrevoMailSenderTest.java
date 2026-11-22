package com.fadikazoura.nextrep.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Brevo is replaced by a mock HTTP server: tests never send real emails. */
class BrevoMailSenderTest {

    private static final String BASE_URL = "https://brevo.test";
    private static final MailProperties PROPERTIES = new MailProperties("test-key", URI.create(BASE_URL),
            "nextrep.fitapp@gmail.com", "NextRep", "nextrep.fitapp@gmail.com", Duration.ofSeconds(5));

    private MockRestServiceServer server;
    private BrevoMailSender sender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        sender = new BrevoMailSender(builder.build(), PROPERTIES);
    }

    @Test
    void sendsTheEmailThroughBrevosApi() {
        server.expect(requestTo(BASE_URL + "/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-key"))
                .andExpect(jsonPath("$.sender.email").value("nextrep.fitapp@gmail.com"))
                .andExpect(jsonPath("$.to[0].email").value("user@example.com"))
                .andExpect(jsonPath("$.replyTo.email").value("reply@example.com"))
                .andExpect(jsonPath("$.subject").value("Hello"))
                .andExpect(jsonPath("$.textContent").value("Plain text"))
                .andRespond(withSuccess("{\"messageId\": \"<1@brevo>\"}", MediaType.APPLICATION_JSON));

        sender.send(new MailMessage("user@example.com", "user", "Hello", "<p>Html</p>", "Plain text", "reply@example.com"));

        server.verify();
    }

    @Test
    void failuresBecomeMailDeliveryException() {
        server.expect(requestTo(BASE_URL + "/v3/smtp/email")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> sender.send(new MailMessage("user@example.com", null, "Hi", "<p>x</p>", "x", null)))
                .isInstanceOf(MailDeliveryException.class);
    }
}
