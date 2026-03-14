package be.vdab.tcoaching.api.contact;

import be.vdab.tcoaching.api.common.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
public class ContactController {
    private static final int HEADER_MAX_LENGTH = 500;
    private static final String INSERT_SQL = """
            INSERT INTO contact_requests
            (name, email, request_type, phone, topic, preferred_time, goal, message, page, lang, ip_address, user_agent, referrer)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final EmailNotificationService emailNotificationService;
    private final CaptchaVerificationService captchaVerificationService;
    private final ClientIpResolver clientIpResolver;

    public ContactController(
            JdbcTemplate jdbcTemplate,
            EmailNotificationService emailNotificationService,
            CaptchaVerificationService captchaVerificationService,
            ClientIpResolver clientIpResolver
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.emailNotificationService = emailNotificationService;
        this.captchaVerificationService = captchaVerificationService;
        this.clientIpResolver = clientIpResolver;
    }

    @Transactional
    @PostMapping(path = "/contact", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> submitContact(
            @Valid @RequestBody ContactRequest request,
            HttpServletRequest httpRequest
    ) {
        if (request.website() != null) {
            return ResponseEntity.noContent().build();
        }

        String ip = clientIpResolver.resolve(httpRequest);
        String userAgent = limitHeader(httpRequest.getHeader("User-Agent"));
        String referrer = limitHeader(httpRequest.getHeader("Referer"));

        captchaVerificationService.verify(request.captchaToken(), ip);

        jdbcTemplate.update(
                INSERT_SQL,
                request.name(),
                request.email(),
                request.requestType(),
                request.phone(),
                request.topic(),
                request.time(),
                request.goal(),
                request.message(),
                request.page(),
                request.lang(),
                ip,
                userAgent,
                referrer
        );

        scheduleNotificationAfterCommit(request, ip, userAgent, referrer);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private void scheduleNotificationAfterCommit(ContactRequest request, String ip, String userAgent, String referrer) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            emailNotificationService.sendContactNotification(request, ip, userAgent, referrer);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emailNotificationService.sendContactNotification(request, ip, userAgent, referrer);
            }
        });
    }

    private String limitHeader(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > HEADER_MAX_LENGTH ? trimmed.substring(0, HEADER_MAX_LENGTH) : trimmed;
    }
}
