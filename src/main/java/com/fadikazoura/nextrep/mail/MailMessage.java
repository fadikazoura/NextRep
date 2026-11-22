package com.fadikazoura.nextrep.mail;

/** One email. {@code replyTo} is optional (used so we can answer a "Contact us" message directly). */
public record MailMessage(String to, String toName, String subject, String html, String text, String replyTo) {
}
