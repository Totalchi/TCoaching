-- noinspection SqlNoDataSourceInspectionForFile
-- noinspection SqlDialectInspectionForFile
-- Expands header-related columns and adds data constraints plus reporting indexes.

ALTER TABLE contact_requests
    MODIFY COLUMN preferred_time VARCHAR(80) NULL,
    MODIFY COLUMN goal VARCHAR(1000) NULL,
    MODIFY COLUMN user_agent VARCHAR(500) NULL,
    MODIFY COLUMN referrer VARCHAR(500) NULL,
    ADD CONSTRAINT chk_contact_requests_request_type
        CHECK (request_type IS NULL OR request_type IN ('intake', 'waitlist', 'lead')),
    ADD CONSTRAINT chk_contact_requests_topic
        CHECK (topic IS NULL OR topic IN ('life', 'pt', 'stress', 'assertive', 'other', 'lead-magnet')),
    ADD CONSTRAINT chk_contact_requests_lang
        CHECK (lang IS NULL OR lang IN ('nl', 'en')),
    ADD CONSTRAINT chk_contact_requests_status
        CHECK (status IN ('new', 'in_progress', 'completed', 'archived')),
    ADD INDEX idx_contact_requests_status_created_at (status, created_at),
    ADD INDEX idx_contact_requests_ip_address (ip_address);

ALTER TABLE page_views
    MODIFY COLUMN referrer VARCHAR(500) NULL,
    MODIFY COLUMN user_agent VARCHAR(500) NULL,
    ADD CONSTRAINT chk_page_views_lang
        CHECK (lang IS NULL OR lang IN ('nl', 'en')),
    ADD INDEX idx_page_views_ip_address (ip_address);

ALTER TABLE analytics_events
    MODIFY COLUMN referrer VARCHAR(500) NULL,
    MODIFY COLUMN user_agent VARCHAR(500) NULL,
    ADD CONSTRAINT chk_analytics_events_lang
        CHECK (lang IS NULL OR lang IN ('nl', 'en'));

SET @has_analytics_events_created_at_idx := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'analytics_events'
      AND INDEX_NAME = 'idx_analytics_events_created_at'
);
SET @create_analytics_events_created_at_idx := IF(
    @has_analytics_events_created_at_idx = 0,
    'ALTER TABLE analytics_events ADD INDEX idx_analytics_events_created_at (created_at)',
    'SELECT 1'
);
PREPARE stmt FROM @create_analytics_events_created_at_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_analytics_events_type_name_idx := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'analytics_events'
      AND INDEX_NAME = 'idx_analytics_events_type_name'
);
SET @create_analytics_events_type_name_idx := IF(
    @has_analytics_events_type_name_idx = 0,
    'ALTER TABLE analytics_events ADD INDEX idx_analytics_events_type_name (event_type, event_name)',
    'SELECT 1'
);
PREPARE stmt FROM @create_analytics_events_type_name_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_analytics_events_ip_address_idx := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'analytics_events'
      AND INDEX_NAME = 'idx_analytics_events_ip_address'
);
SET @create_analytics_events_ip_address_idx := IF(
    @has_analytics_events_ip_address_idx = 0,
    'ALTER TABLE analytics_events ADD INDEX idx_analytics_events_ip_address (ip_address)',
    'SELECT 1'
);
PREPARE stmt FROM @create_analytics_events_ip_address_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
