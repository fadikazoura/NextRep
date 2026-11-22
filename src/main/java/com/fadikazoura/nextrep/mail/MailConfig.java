package com.fadikazoura.nextrep.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    /** Brevo when a key is configured, otherwise the log-only sender. */
    @Bean
    MailSender mailSender(MailProperties properties) {
        if (!properties.enabled()) {
            log.warn("BREVO_API_KEY is not set: emails will be written to the log instead of being sent");
            return new LoggingMailSender();
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());
        RestClient client = RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
        return new BrevoMailSender(client, properties);
    }
}
