package com.fadikazoura.nextrep.mail;

import org.springframework.web.util.HtmlUtils;

/**
 * The emails NextRep sends. Anything a user typed is HTML-escaped before it goes into an email,
 * so a message like "<script>…" shows up as text and can't inject markup.
 */
public final class MailTemplates {

    private MailTemplates() {
    }

    public static MailMessage passwordResetCode(String email, String username, String code, long minutes) {
        String name = HtmlUtils.htmlEscape(username);
        String html = layout("""
                <p style="margin:0 0 12px">Hi %s,</p>
                <p style="margin:0 0 20px">Use this code to choose a new NextRep password:</p>
                <div style="font-size:34px;font-weight:800;letter-spacing:10px;padding:16px 0;text-align:center;
                            background:#0c0e11;color:#c6ff3d;border-radius:12px">%s</div>
                <p style="margin:20px 0 0;color:#5d6675;font-size:14px">The code expires in %d minutes.
                If you didn't ask to reset your password, you can ignore this email — your password stays the same.</p>
                """.formatted(name, code, minutes));
        String text = """
                Hi %s,

                Your NextRep password reset code: %s
                It expires in %d minutes.

                If you didn't ask for this, ignore this email.""".formatted(username, code, minutes);
        return new MailMessage(email, username, "Your NextRep code: " + code, html, text, null);
    }

    public static MailMessage contactNotification(String supportInbox, String username, String userEmail,
                                                  String topic, String message) {
        String html = layout("""
                <p style="margin:0 0 6px"><b>From:</b> %s &lt;%s&gt;</p>
                <p style="margin:0 0 16px"><b>Topic:</b> %s</p>
                <div style="white-space:pre-wrap;padding:14px;border-radius:10px;background:#eef0f3">%s</div>
                <p style="margin:16px 0 0;color:#5d6675;font-size:13px">Reply to this email to answer the user.</p>
                """.formatted(HtmlUtils.htmlEscape(username), HtmlUtils.htmlEscape(userEmail),
                HtmlUtils.htmlEscape(topic), HtmlUtils.htmlEscape(message)));
        String text = "From: %s <%s>%nTopic: %s%n%n%s".formatted(username, userEmail, topic, message);
        return new MailMessage(supportInbox, "NextRep support", "[NextRep] " + topic + " from " + username,
                html, text, userEmail);
    }

    private static String layout(String content) {
        return """
                <div style="font-family:Arial,sans-serif;background:#f5f6f8;padding:24px">
                  <div style="max-width:480px;margin:0 auto;background:#ffffff;border-radius:16px;padding:28px;color:#14171c">
                    <div style="font-size:22px;font-weight:800;margin-bottom:20px">NEXT<span style="color:#4d7c0f">REP</span></div>
                    %s
                  </div>
                </div>""".formatted(content);
    }
}
