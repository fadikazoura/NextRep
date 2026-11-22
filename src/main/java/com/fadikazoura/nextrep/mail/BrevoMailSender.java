package com.fadikazoura.nextrep.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Sends emails through Brevo's transactional email API (POST /v3/smtp/email). */
@Slf4j
public class BrevoMailSender implements MailSender {

    private final RestClient restClient;
    private final MailProperties properties;

    public BrevoMailSender(RestClient restClient, MailProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public void send(MailMessage message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sender", Map.of("name", properties.senderName(), "email", properties.senderEmail()));
        body.put("to", List.of(message.toName() == null
                ? Map.of("email", message.to())
                : Map.of("email", message.to(), "name", message.toName())));
        if (message.replyTo() != null) body.put("replyTo", Map.of("email", message.replyTo()));
        body.put("subject", message.subject());
        body.put("htmlContent", message.html());
        body.put("textContent", message.text());

        try {
            restClient.post()
                    .uri("/v3/smtp/email")
                    .header("api-key", properties.brevoApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email '{}' sent", message.subject()); // never log the address or the content
        } catch (RestClientException e) {
            throw new MailDeliveryException("Brevo could not send the email: " + e.getMessage(), e);
        }
    }
}
