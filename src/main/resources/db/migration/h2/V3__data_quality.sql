-- Mirrors MySQL data hardening for H2 tests: wider header columns, constraints, and indexes.
CREATE TABLE IF NOT EXISTS analytics_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    path VARCHAR(200) NOT NULL,
    title VARCHAR(200),
    referrer VARCHAR(255),
    lang VARCHAR(10),
    event_type VARCHAR(40),
    event_name VARCHAR(80),
    event_value VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE contact_requests ALTER COLUMN preferred_time SET DATA TYPE VARCHAR(80);
ALTER TABLE contact_requests ALTER COLUMN goal SET DATA TYPE VARCHAR(1000);
ALTER TABLE contact_requests ALTER COLUMN user_agent SET DATA TYPE VARCHAR(500);
ALTER TABLE contact_requests ALTER COLUMN referrer SET DATA TYPE VARCHAR(500);

ALTER TABLE page_views ALTER COLUMN referrer SET DATA TYPE VARCHAR(500);
ALTER TABLE page_views ALTER COLUMN user_agent SET DATA TYPE VARCHAR(500);

ALTER TABLE analytics_events ALTER COLUMN referrer SET DATA TYPE VARCHAR(500);
ALTER TABLE analytics_events ALTER COLUMN user_agent SET DATA TYPE VARCHAR(500);

ALTER TABLE contact_requests
    ADD CONSTRAINT chk_contact_requests_request_type
    CHECK (request_type IS NULL OR request_type IN ('intake', 'waitlist', 'lead'));

ALTER TABLE contact_requests
    ADD CONSTRAINT chk_contact_requests_topic
    CHECK (topic IS NULL OR topic IN ('life', 'pt', 'stress', 'assertive', 'other', 'lead-magnet'));

ALTER TABLE contact_requests
    ADD CONSTRAINT chk_contact_requests_lang
    CHECK (lang IS NULL OR lang IN ('nl', 'en'));

ALTER TABLE contact_requests
    ADD CONSTRAINT chk_contact_requests_status
    CHECK (status IN ('new', 'in_progress', 'completed', 'archived'));

ALTER TABLE page_views
    ADD CONSTRAINT chk_page_views_lang
    CHECK (lang IS NULL OR lang IN ('nl', 'en'));

ALTER TABLE analytics_events
    ADD CONSTRAINT chk_analytics_events_lang
    CHECK (lang IS NULL OR lang IN ('nl', 'en'));

CREATE INDEX IF NOT EXISTS idx_contact_requests_status_created_at ON contact_requests (status, created_at);
CREATE INDEX IF NOT EXISTS idx_contact_requests_ip_address ON contact_requests (ip_address);
CREATE INDEX IF NOT EXISTS idx_page_views_ip_address ON page_views (ip_address);
CREATE INDEX IF NOT EXISTS idx_analytics_events_ip_address ON analytics_events (ip_address);
