package com.fadikazoura.nextrep.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

/**
 * Email settings. The Brevo API key comes from the BREVO_API_KEY environment variable and is never committed.
 * Without a key, emails are written to the log instead of being sent (handy in development and tests).
 */
@ConfigurationProperties("nextrep.mail")
public record MailProperties(String brevoApiKey, URI baseUrl, String senderEmail, String senderName,
                             String supportInbox, Duration timeout) {

    public boolean enabled() {
        return brevoApiKey != null && !brevoApiKey.isBlank();
    }
}
