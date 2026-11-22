package com.fadikazoura.nextrep.mail;

/** Sends emails. The rest of the app depends on this interface, not on Brevo (easy to swap or mock in tests). */
public interface MailSender {

    void send(MailMessage message);
}
