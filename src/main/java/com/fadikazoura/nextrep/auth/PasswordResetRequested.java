package com.fadikazoura.nextrep.auth;

/** Published inside the transaction; the email is sent only after it commits (see MailNotifications). */
public record PasswordResetRequested(String email, String username, String code, long validMinutes) {
}
