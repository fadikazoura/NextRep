package com.fadikazoura.nextrep.mail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MailTemplatesTest {

    @Test
    void resetEmailContainsTheCode() {
        MailMessage mail = MailTemplates.passwordResetCode("a@example.com", "fadi", "123456", 15);

        assertThat(mail.subject()).contains("123456");
        assertThat(mail.html()).contains("123456").contains("15 minutes");
        assertThat(mail.text()).contains("123456");
    }

    @Test
    void userTextIsEscapedInTheHtml() {
        MailMessage mail = MailTemplates.contactNotification("team@example.com", "fadi", "a@example.com",
                "BUG", "<script>alert(1)</script>");

        assertThat(mail.html()).doesNotContain("<script>").contains("&lt;script&gt;");
        assertThat(mail.replyTo()).isEqualTo("a@example.com");
    }
}
