package be.vdab.tcoaching.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Component
@SuppressWarnings({"SqlNoDataSourceInspection", "SqlDialectInspection"})
public class ClientTokenCleanupJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientTokenCleanupJob.class);

    private final JdbcTemplate jdbcTemplate;

    public ClientTokenCleanupJob(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "0 17 3 * * *")
    public void purgeExpiredTokens() {
        int deleted = jdbcTemplate.update(
                "DELETE FROM client_tokens WHERE used = TRUE OR expires_at < ?",
                Timestamp.valueOf(LocalDateTime.now())
        );
        if (deleted > 0) {
            LOGGER.info("Purged {} expired or used client tokens", deleted);
        }
    }
}
