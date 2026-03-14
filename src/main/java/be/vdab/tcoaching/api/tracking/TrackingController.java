package be.vdab.tcoaching.api.tracking;

import be.vdab.tcoaching.api.common.ClientIpResolver;
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
@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
public class TrackingController {
    private static final int HEADER_MAX_LENGTH = 500;
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
    private final ClientIpResolver clientIpResolver;

    public TrackingController(
            JdbcTemplate jdbcTemplate,
            ClientIpResolver clientIpResolver
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping(path = "/track", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> trackPageView(
            @Valid @RequestBody PageViewRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = clientIpResolver.resolve(httpRequest);
        String userAgent = limitHeader(httpRequest.getHeader("User-Agent"));
        String referrerHeader = limitHeader(httpRequest.getHeader("Referer"));
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
