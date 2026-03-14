-- noinspection SqlNoDataSourceInspectionForFile
-- noinspection SqlDialectInspectionForFile
-- Mirrors MySQL data hardening for H2 tests: wider header columns, constraints, and indexes.

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
CREATE INDEX IF NOT EXISTS idx_analytics_events_created_at ON analytics_events (created_at);
CREATE INDEX IF NOT EXISTS idx_analytics_events_type_name ON analytics_events (event_type, event_name);
CREATE INDEX IF NOT EXISTS idx_analytics_events_ip_address ON analytics_events (ip_address);
