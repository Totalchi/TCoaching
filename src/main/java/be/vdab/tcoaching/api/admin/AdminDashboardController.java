package be.vdab.tcoaching.api.admin;

import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {
    private static final String RECENT_CONTACTS_SQL = """
            SELECT name,
                   email,
                   COALESCE(status, 'new') AS status,
                   COALESCE(request_type, '') AS request_type,
                   COALESCE(topic, '') AS topic,
                   COALESCE(page, '') AS page,
                   created_at
            FROM contact_requests
            WHERE deleted_at IS NULL
            ORDER BY created_at DESC
            LIMIT 8
            """;
    private static final String TOP_PAGES_SQL = """
            SELECT path, COUNT(*) AS visits, MAX(created_at) AS last_seen
            FROM page_views
            WHERE created_at >= ?
            GROUP BY path
            ORDER BY visits DESC, path ASC
            LIMIT 8
            """;
    private static final String TOP_EVENTS_SQL = """
            SELECT COALESCE(event_type, '') AS event_type,
                   COALESCE(event_name, '') AS event_name,
                   COUNT(*) AS total,
                   MAX(created_at) AS last_seen
            FROM analytics_events
            WHERE created_at >= ?
            GROUP BY event_type, event_name
            ORDER BY total DESC, event_type ASC, event_name ASC
            LIMIT 10
            """;
    private static final String CONTACTS_SQL = """
            SELECT id,
                   name,
                   email,
                   COALESCE(status, 'new') AS status,
                   COALESCE(request_type, '') AS request_type,
                   COALESCE(topic, '') AS topic,
                   COALESCE(page, '') AS page,
                   COALESCE(phone, '') AS phone,
                   COALESCE(preferred_time, '') AS preferred_time,
                   COALESCE(goal, '') AS goal,
                   COALESCE(message, '') AS message,
                   COALESCE(admin_notes, '') AS admin_notes,
                   created_at,
                   updated_at
            FROM contact_requests
            WHERE deleted_at IS NULL
            ORDER BY created_at DESC
            LIMIT 40
            """;
    private static final String UPDATE_CONTACT_SQL = """
            UPDATE contact_requests
            SET status = COALESCE(?, status),
                admin_notes = ?,
                updated_at = CURRENT_TIMESTAMP,
                deleted_at = CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE deleted_at END
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public AdminDashboardController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard() {
        Instant generatedAt = Instant.now();
        Timestamp since = Timestamp.from(generatedAt.minus(30, ChronoUnit.DAYS));

        Overview overview = new Overview(
                queryForCount("SELECT COUNT(*) FROM contact_requests WHERE deleted_at IS NULL"),
                queryForCount("SELECT COUNT(*) FROM contact_requests WHERE deleted_at IS NULL AND created_at >= ?", since),
                queryForCount("SELECT COUNT(*) FROM page_views WHERE created_at >= ?", since),
                queryForCount("SELECT COUNT(*) FROM analytics_events WHERE created_at >= ?", since),
                queryForCount(
                        "SELECT COUNT(*) FROM analytics_events WHERE event_type = ? AND created_at >= ?",
                        "cta_click",
                        since
                )
        );

        List<RecentContact> recentContacts = jdbcTemplate.query(
                RECENT_CONTACTS_SQL,
                (resultSet, rowNum) -> new RecentContact(
                        resultSet.getString("name"),
                        resultSet.getString("email"),
                        resultSet.getString("status"),
                        resultSet.getString("request_type"),
                        resultSet.getString("topic"),
                        resultSet.getString("page"),
                        resultSet.getTimestamp("created_at").toInstant()
                )
        );

        List<PageSummary> topPages = jdbcTemplate.query(
                TOP_PAGES_SQL,
                (resultSet, rowNum) -> new PageSummary(
                        resultSet.getString("path"),
                        resultSet.getLong("visits"),
                        resultSet.getTimestamp("last_seen").toInstant()
                ),
                since
        );

        List<EventSummary> topEvents = jdbcTemplate.query(
                TOP_EVENTS_SQL,
                (resultSet, rowNum) -> new EventSummary(
                        resultSet.getString("event_type"),
                        resultSet.getString("event_name"),
                        resultSet.getLong("total"),
                        resultSet.getTimestamp("last_seen").toInstant()
                ),
                since
        );

        return new DashboardResponse(generatedAt, overview, recentContacts, topPages, topEvents);
    }

    @GetMapping("/contacts")
    public List<ContactLead> getContacts() {
        return jdbcTemplate.query(
                CONTACTS_SQL,
                (resultSet, rowNum) -> new ContactLead(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getString("email"),
                        resultSet.getString("status"),
                        resultSet.getString("request_type"),
                        resultSet.getString("topic"),
                        resultSet.getString("page"),
                        resultSet.getString("phone"),
                        resultSet.getString("preferred_time"),
                        resultSet.getString("goal"),
                        resultSet.getString("message"),
                        resultSet.getString("admin_notes"),
                        resultSet.getTimestamp("created_at").toInstant(),
                        resultSet.getTimestamp("updated_at").toInstant()
                )
        );
    }

    @PatchMapping("/contacts/{id}")
    public void updateContact(
            @PathVariable long id,
            @Valid @RequestBody LeadUpdateRequest request
    ) {
        boolean archived = Boolean.TRUE.equals(request.archived())
                || "archived".equals(request.status());
        jdbcTemplate.update(
                UPDATE_CONTACT_SQL,
                archived ? "archived" : request.status(),
                request.adminNotes(),
                archived,
                id
        );
    }

    private long queryForCount(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    public record DashboardResponse(
            Instant generatedAt,
            Overview overview,
            List<RecentContact> recentContacts,
            List<PageSummary> topPages,
            List<EventSummary> topEvents
    ) {
    }

    public record Overview(
            long totalLeads,
            long leadsLast30Days,
            long pageViewsLast30Days,
            long eventsLast30Days,
            long ctaClicksLast30Days
    ) {
    }

    public record RecentContact(
            String name,
            String email,
            String status,
            String requestType,
            String topic,
            String page,
            Instant createdAt
    ) {
    }

    public record ContactLead(
            long id,
            String name,
            String email,
            String status,
            String requestType,
            String topic,
            String page,
            String phone,
            String preferredTime,
            String goal,
            String message,
            String adminNotes,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record PageSummary(
            String path,
            long visits,
            Instant lastSeen
    ) {
    }

    public record EventSummary(
            String eventType,
            String eventName,
            long total,
            Instant lastSeen
    ) {
    }
}
