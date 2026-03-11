package be.vdab.tcoaching.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ContactController {
    private static final String INSERT_SQL = """
            INSERT INTO contact_requests
            (name, email, request_type, phone, topic, preferred_time, goal, message, page, lang, ip_address, user_agent, referrer)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final EmailNotificationService emailNotificationService;
    private final CaptchaVerificationService captchaVerificationService;

    public ContactController(
            JdbcTemplate jdbcTemplate,
            EmailNotificationService emailNotificationService,
            CaptchaVerificationService captchaVerificationService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.emailNotificationService = emailNotificationService;
        this.captchaVerificationService = captchaVerificationService;
    }

    @PostMapping(path = "/contact", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> submitContact(
            @Valid @RequestBody ContactRequest request,
            HttpServletRequest httpRequest
    ) {
        if (request.website() != null) {
            return ResponseEntity.noContent().build();
        }

        String ip = resolveClientIp(httpRequest);
        String userAgent = limitHeader(httpRequest.getHeader("User-Agent"), 500);
        String referrer = limitHeader(httpRequest.getHeader("Referer"), 500);

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

        emailNotificationService.sendContactNotification(request, ip, userAgent, referrer);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String limitHeader(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }
}
