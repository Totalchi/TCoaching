package be.vdab.tcoaching.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String to;
    private final String from;
    private final String subject;
    private final String username;
    private final String password;

    public EmailNotificationService(
            JavaMailSender mailSender,
            @Value("${contact.notification.enabled:true}") boolean enabled,
            @Value("${contact.notification.to:}") String to,
            @Value("${contact.notification.from:}") String from,
            @Value("${contact.notification.subject:New contact request}") String subject,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password
    ) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.to = to;
        this.from = from;
        this.subject = subject;
        this.username = username;
        this.password = password;
    }

    @Async("notificationTaskExecutor")
    public void sendContactNotification(ContactRequest request, String ip, String userAgent, String referrer) {
        if (!isConfigured()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(from);
        message.setSubject(subject);
        message.setText(buildBody(request, ip, userAgent, referrer));

        try {
            mailSender.send(message);
        } catch (RuntimeException ex) {
            LOGGER.warn("Contact notification email failed: {}", ex.getMessage());
        }
    }

    private boolean isConfigured() {
        return enabled
                && hasText(username)
                && hasText(password)
                && hasText(to)
                && hasText(from);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String buildBody(ContactRequest request, String ip, String userAgent, String referrer) {
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

    private void appendLine(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append(label).append(": ").append(value).append(System.lineSeparator());
    }
}
