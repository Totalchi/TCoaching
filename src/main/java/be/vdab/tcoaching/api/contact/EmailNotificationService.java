package be.vdab.tcoaching.api.contact;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

@Service
public class EmailNotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String to;
    private final String from;
    private final String subject;

    public EmailNotificationService(
            JavaMailSender mailSender,
            @Value("${contact.notification.enabled:true}") boolean enabled,
            @Value("${contact.notification.to:}") String to,
            @Value("${contact.notification.from:}") String from,
            @Value("${contact.notification.subject:New contact request}") String subject
    ) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.to = to;
        this.from = from;
        this.subject = subject;
    }

    @Async("notificationTaskExecutor")
    public void sendContactNotification(ContactRequest request, String ip, String userAgent, String referrer) {
        if (!isConfigured()) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject(subject);
            helper.setText(buildPlainTextBody(request, ip, userAgent, referrer), buildHtmlBody(request, ip, userAgent, referrer));
            mailSender.send(message);
        } catch (MessagingException | RuntimeException ex) {
            LOGGER.warn("Contact notification email failed: {}", ex.getMessage());
        }
    }

    private boolean isConfigured() {
        return enabled
                && hasText(to)
                && hasText(from);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String buildPlainTextBody(ContactRequest request, String ip, String userAgent, String referrer) {
        StringBuilder builder = new StringBuilder(512);
        builder.append("New contact request").append(System.lineSeparator()).append(System.lineSeparator());
        appendLine(builder, "Name", request.name());
        appendLine(builder, "Email", request.email());
        appendLine(builder, "Phone", request.phone());
        appendLine(builder, "Topic", request.topic());
        appendLine(builder, "Preferred time", request.time());
        appendLine(builder, "Goal", request.goal());
        appendLine(builder, "Message", request.message());
        appendLine(builder, "Page", request.page());
        appendLine(builder, "Language", request.lang());
        appendLine(builder, "IP", ip);
        appendLine(builder, "User agent", userAgent);
        appendLine(builder, "Referrer", referrer);
        return builder.toString();
    }

    private String buildHtmlBody(ContactRequest request, String ip, String userAgent, String referrer) {
        StringBuilder builder = new StringBuilder(2048);
        builder.append("""
                <html>
                <body style="margin:0;padding:24px;background:#f5f1ea;color:#241e17;font-family:Arial,sans-serif;">
                  <div style="max-width:720px;margin:0 auto;background:#fff9f1;border:1px solid #e6d8bf;border-radius:18px;overflow:hidden;">
                    <div style="padding:24px 28px;background:#241e17;color:#f5f1ea;">
                      <p style="margin:0 0 8px;font-size:12px;letter-spacing:0.18em;text-transform:uppercase;color:#d8bb72;">TCoaching</p>
                      <h1 style="margin:0;font-size:28px;line-height:1.2;font-weight:700;">New contact request</h1>
                    </div>
                    <div style="padding:24px 28px;">
                      <p style="margin:0 0 20px;font-size:15px;line-height:1.6;color:#5a4a34;">A new lead was submitted via the website.</p>
                      <table role="presentation" style="width:100%;border-collapse:collapse;">
                """);
        appendHtmlRow(builder, "Name", request.name());
        appendHtmlRow(builder, "Email", request.email());
        appendHtmlRow(builder, "Phone", request.phone());
        appendHtmlRow(builder, "Topic", request.topic());
        appendHtmlRow(builder, "Preferred time", request.time());
        appendHtmlRow(builder, "Goal", request.goal());
        appendHtmlRow(builder, "Message", request.message());
        appendHtmlRow(builder, "Page", request.page());
        appendHtmlRow(builder, "Language", request.lang());
        appendHtmlRow(builder, "Stored IP", ip);
        appendHtmlRow(builder, "User agent", userAgent);
        appendHtmlRow(builder, "Referrer", referrer);
        builder.append("""
                      </table>
                    </div>
                  </div>
                </body>
                </html>
                """);
        return builder.toString();
    }

    private void appendHtmlRow(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append("""
                <tr>
                  <td style="padding:10px 0;border-bottom:1px solid #eadfc9;width:180px;vertical-align:top;font-weight:700;color:#241e17;">""")
                .append(HtmlUtils.htmlEscape(label))
                .append("""
                </td>
                  <td style="padding:10px 0;border-bottom:1px solid #eadfc9;vertical-align:top;color:#4a3b28;white-space:pre-wrap;">""")
                .append(HtmlUtils.htmlEscape(value))
                .append("""
                </td>
                </tr>
                """);
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append(label).append(": ").append(value).append(System.lineSeparator());
    }
}
