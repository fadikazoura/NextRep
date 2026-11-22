package com.fadikazoura.nextrep.feedback;

/** Published when a "Contact us" message is saved; the email to the team is sent after the commit. */
public record ContactMessageReceived(Long messageId, String username, String email, ContactTopic topic, String message) {
}
