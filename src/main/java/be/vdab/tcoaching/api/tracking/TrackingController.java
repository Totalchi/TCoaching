package be.vdab.tcoaching.api.tracking;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TrackingController {
    private static final String INSERT_PAGE_VIEW_SQL = """
            INSERT INTO page_views
            (path, title, referrer, lang, ip_address, user_agent)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String INSERT_EVENT_SQL = """
            INSERT INTO analytics_events
            (path, title, referrer, lang, event_type, event_name, event_value, ip_address, user_agent)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public TrackingController(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping(path = "/track", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> trackPageView(
            @Valid @RequestBody PageViewRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = resolveClientIp(httpRequest);
        String userAgent = limitHeader(httpRequest.getHeader("User-Agent"), 500);
        String referrerHeader = limitHeader(httpRequest.getHeader("Referer"), 500);
        String referrer = request.referrer() != null ? request.referrer() : referrerHeader;

        if (request.isPageView()) {
            jdbcTemplate.update(
                    INSERT_PAGE_VIEW_SQL,
                    request.path(),
                    request.title(),
                    referrer,
                    request.lang(),
                    ip,
                    userAgent
            );
        } else {
            jdbcTemplate.update(
                    INSERT_EVENT_SQL,
                    request.path(),
                    request.title(),
                    referrer,
                    request.lang(),
                    request.eventType(),
                    request.eventName(),
                    request.eventValue(),
                    ip,
                    userAgent
            );
        }

        return ResponseEntity.noContent().build();
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
