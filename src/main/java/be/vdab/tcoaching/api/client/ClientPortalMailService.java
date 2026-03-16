package be.vdab.tcoaching.api.client;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

@Service
public class ClientPortalMailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientPortalMailService.class);

    private final JavaMailSender mailSender;
    private final String publicBaseUrl;
    private final String from;
    private final String replyTo;

    public ClientPortalMailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${portal.client.public-base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${portal.client.mail.from:}") String from,
            @Value("${portal.client.mail.reply-to:}") String replyTo
    ) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.publicBaseUrl = trimTrailingSlash(publicBaseUrl);
        this.from = from == null ? "" : from.trim();
        this.replyTo = replyTo == null ? "" : replyTo.trim();
    }

    @Async("notificationTaskExecutor")
    public void sendVerificationEmail(String email, String firstName, String lang, String token) {
        if (mailUnavailable()) {
            return;
        }
        String subject = "en".equalsIgnoreCase(lang) ? "Verify your TCoaching account" : "Bevestig je TCoaching-account";
        String actionUrl = publicBaseUrl + "/api/client/verify-email?token=" + token;
        String greetingName = hasText(firstName) ? firstName.trim() : email;
        String intro = "en".equalsIgnoreCase(lang)
                ? "Confirm your email address to activate your client portal."
                : "Bevestig je e-mailadres om je klantenportaal te activeren.";
        String button = "en".equalsIgnoreCase(lang) ? "Verify account" : "Account bevestigen";
        sendMail(email, subject, greetingName, intro, actionUrl, button);
    }

    @Async("notificationTaskExecutor")
    public void sendPasswordResetEmail(String email, String firstName, String lang, String token) {
        if (mailUnavailable()) {
            return;
        }
        String subject = "en".equalsIgnoreCase(lang) ? "Reset your TCoaching password" : "Reset je TCoaching-wachtwoord";
        String actionUrl = publicBaseUrl + "/wachtwoord-reset.html?token=" + token;
        String greetingName = hasText(firstName) ? firstName.trim() : email;
        String intro = "en".equalsIgnoreCase(lang)
                ? "Use the link below to choose a new password."
                : "Gebruik de link hieronder om een nieuw wachtwoord te kiezen.";
        String button = "en".equalsIgnoreCase(lang) ? "Choose new password" : "Kies nieuw wachtwoord";
        sendMail(email, subject, greetingName, intro, actionUrl, button);
    }

    private void sendMail(String to, String subject, String greetingName, String intro, String actionUrl, String buttonLabel) {
        try {
            if (mailSender == null) {
                return;
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setFrom(from);
            if (hasText(replyTo)) {
                helper.setReplyTo(replyTo);
            }
            helper.setSubject(subject);
            helper.setText(buildPlainTextBody(greetingName, intro, actionUrl), buildHtmlBody(greetingName, intro, actionUrl, buttonLabel));
            mailSender.send(message);
        } catch (MessagingException | RuntimeException ex) {
            LOGGER.warn("Client portal email failed: {}", ex.getMessage());
        }
    }

    private String buildPlainTextBody(String greetingName, String intro, String actionUrl) {
        return "Hallo " + greetingName + System.lineSeparator() + System.lineSeparator()
                + intro + System.lineSeparator()
                + actionUrl + System.lineSeparator() + System.lineSeparator()
                + "TCoaching";
    }

    private String buildHtmlBody(String greetingName, String intro, String actionUrl, String buttonLabel) {
        String safeButton = HtmlUtils.htmlEscape(buttonLabel);
        String safeGreeting = HtmlUtils.htmlEscape(greetingName);
        String safeIntro = HtmlUtils.htmlEscape(intro);
        String safeUrl = HtmlUtils.htmlEscape(actionUrl);
        return """
                <html>
                <body style="margin:0;padding:24px;background:#f5f1ea;color:#241e17;font-family:Arial,sans-serif;">
                  <div style="max-width:640px;margin:0 auto;background:#fff9f1;border:1px solid #e6d8bf;border-radius:18px;overflow:hidden;">
                    <div style="padding:24px 28px;background:#241e17;color:#f5f1ea;">
                      <p style="margin:0 0 8px;font-size:12px;letter-spacing:0.18em;text-transform:uppercase;color:#d8bb72;">TCoaching</p>
                      <h1 style="margin:0;font-size:26px;line-height:1.2;font-weight:700;">%s</h1>
                    </div>
                    <div style="padding:24px 28px;">
                      <p style="margin:0 0 12px;font-size:15px;line-height:1.6;color:#5a4a34;">Hallo %s</p>
                      <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#5a4a34;">%s</p>
                      <p style="margin:0 0 24px;">
                        <a href="%s" style="display:inline-block;padding:14px 20px;border-radius:999px;background:#241e17;color:#f5f1ea;text-decoration:none;font-weight:700;">%s</a>
                      </p>
                      <p style="margin:0;font-size:13px;line-height:1.6;color:#7c6c57;">%s</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(safeButton, safeGreeting, safeIntro, safeUrl, safeButton, safeUrl);
    }

    private boolean mailUnavailable() {
        return mailSender == null || !hasText(from) || !hasText(publicBaseUrl);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("/+$", "");
    }
}
