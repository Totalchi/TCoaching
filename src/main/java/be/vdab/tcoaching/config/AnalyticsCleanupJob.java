package be.vdab.tcoaching.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class AnalyticsCleanupJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsCleanupJob.class);

    private final JdbcTemplate jdbcTemplate;
    private final int retentionDays;

    public AnalyticsCleanupJob(
            JdbcTemplate jdbcTemplate,
            @Value("${analytics.retention.days:90}") int retentionDays
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.retentionDays = Math.max(1, retentionDays);
    }

    @Scheduled(cron = "0 30 3 * * *")
    void cleanupOldAnalytics() {
        Timestamp cutoff = Timestamp.from(Instant.now().minus(retentionDays, ChronoUnit.DAYS));
        int pageViewsDeleted = jdbcTemplate.update("DELETE FROM page_views WHERE created_at < ?", cutoff);
        int eventsDeleted = jdbcTemplate.update("DELETE FROM analytics_events WHERE created_at < ?", cutoff);

        if (pageViewsDeleted > 0 || eventsDeleted > 0) {
            LOGGER.info(
                    "Deleted {} page views and {} analytics events older than {} days.",
                    pageViewsDeleted,
                    eventsDeleted,
                    retentionDays
            );
        }
    }
}
