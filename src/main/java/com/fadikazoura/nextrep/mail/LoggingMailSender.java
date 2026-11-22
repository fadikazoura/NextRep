package com.fadikazoura.nextrep.mail;

import lombok.extern.slf4j.Slf4j;

/** Development fallback when no Brevo key is set: the email is printed to the log instead of being sent. */
@Slf4j
public class LoggingMailSender implements MailSender {

    @Override
    public void send(MailMessage message) {
        log.info("""
                [DEV] Email not sent (no BREVO_API_KEY). To: {}
                Subject: {}
                {}""", message.to(), message.subject(), message.text());
    }
}
