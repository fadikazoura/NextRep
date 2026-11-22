package com.fadikazoura.nextrep.mail;

import com.fadikazoura.nextrep.auth.PasswordResetRequested;
import com.fadikazoura.nextrep.feedback.ContactMessageReceived;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends emails AFTER the database transaction commits. If the transaction rolls back, no email goes out
 * (no code for a reset that was never saved). If the email fails, the saved data stays and the error is logged:
 * the user can simply ask for a new code, and a contact message is still in the database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailNotifications {

    private final MailSender mailSender;
    private final MailProperties properties;

    @TransactionalEventListener
    public void onPasswordResetRequested(PasswordResetRequested event) {
        send(MailTemplates.passwordResetCode(event.email(), event.username(), event.code(), event.validMinutes()));
    }

    @TransactionalEventListener
    public void onContactMessage(ContactMessageReceived event) {
        send(MailTemplates.contactNotification(properties.supportInbox(), event.username(), event.email(),
                event.topic().name(), event.message()));
    }

    private void send(MailMessage message) {
        try {
            mailSender.send(message);
        } catch (RuntimeException e) {
            log.error("Email '{}' could not be sent: {}", message.subject(), e.getMessage());
        }
    }
}
