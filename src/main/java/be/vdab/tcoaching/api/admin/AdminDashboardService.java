package be.vdab.tcoaching.api.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
public class AdminDashboardService {
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
            LIMIT ?
            OFFSET ?
            """;
    private static final String CONTACTS_TOTAL_SQL = """
            SELECT COUNT(*)
            FROM contact_requests
            WHERE deleted_at IS NULL
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

    public AdminDashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AdminDashboardData.DashboardResponse getDashboard() {
        Instant generatedAt = Instant.now();
        Timestamp since = Timestamp.from(generatedAt.minus(30, ChronoUnit.DAYS));

        AdminDashboardData.Overview overview = new AdminDashboardData.Overview(
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

        List<AdminDashboardData.RecentContact> recentContacts = jdbcTemplate.query(
                RECENT_CONTACTS_SQL,
                AdminDashboardService::mapRecentContact
        );

        List<AdminDashboardData.PageSummary> topPages = jdbcTemplate.query(
                TOP_PAGES_SQL,
                AdminDashboardService::mapPageSummary,
                since
        );

        List<AdminDashboardData.EventSummary> topEvents = jdbcTemplate.query(
                TOP_EVENTS_SQL,
                AdminDashboardService::mapEventSummary,
                since
        );

        return new AdminDashboardData.DashboardResponse(generatedAt, overview, recentContacts, topPages, topEvents);
    }

    public AdminDashboardData.ContactLeadPage getContacts(int offset, int limit) {
        int normalizedOffset = Math.max(0, offset);
        int normalizedLimit = Math.clamp(limit, 1, 100);
        List<AdminDashboardData.ContactLead> items = jdbcTemplate.query(
                CONTACTS_SQL,
                AdminDashboardService::mapContactLead,
                normalizedLimit,
                normalizedOffset
        );
        long total = queryForCount(CONTACTS_TOTAL_SQL);
        return new AdminDashboardData.ContactLeadPage(
                items,
                total,
                normalizedOffset,
                normalizedLimit,
                normalizedOffset + items.size() < total
        );
    }

    public void updateContact(long id, LeadUpdateRequest request) {
        boolean archived = Boolean.TRUE.equals(request.archived())
                || "archived".equals(request.status());
        int updatedRows = jdbcTemplate.update(
                UPDATE_CONTACT_SQL,
                archived ? "archived" : request.status(),
                request.adminNotes(),
                archived,
                id
        );
        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact lead not found");
        }
    }

    private long queryForCount(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private static AdminDashboardData.RecentContact mapRecentContact(
            java.sql.ResultSet resultSet,
            @SuppressWarnings("unused") int rowNum
    ) throws java.sql.SQLException {
        return new AdminDashboardData.RecentContact(
                resultSet.getString("name"),
                resultSet.getString("email"),
                resultSet.getString("status"),
                resultSet.getString("request_type"),
                resultSet.getString("topic"),
                resultSet.getString("page"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }

    private static AdminDashboardData.PageSummary mapPageSummary(
            java.sql.ResultSet resultSet,
            @SuppressWarnings("unused") int rowNum
    ) throws java.sql.SQLException {
        return new AdminDashboardData.PageSummary(
                resultSet.getString("path"),
                resultSet.getLong("visits"),
                resultSet.getTimestamp("last_seen").toInstant()
        );
    }

    private static AdminDashboardData.EventSummary mapEventSummary(
            java.sql.ResultSet resultSet,
            @SuppressWarnings("unused") int rowNum
    ) throws java.sql.SQLException {
        return new AdminDashboardData.EventSummary(
                resultSet.getString("event_type"),
                resultSet.getString("event_name"),
                resultSet.getLong("total"),
                resultSet.getTimestamp("last_seen").toInstant()
        );
    }

    private static AdminDashboardData.ContactLead mapContactLead(
            java.sql.ResultSet resultSet,
            @SuppressWarnings("unused") int rowNum
    ) throws java.sql.SQLException {
        return new AdminDashboardData.ContactLead(
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
        );
    }
}
