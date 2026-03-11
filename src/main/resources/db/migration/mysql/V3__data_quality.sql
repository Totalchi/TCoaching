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
        CHECK (lang IS NULL OR lang IN ('nl', 'en')),
    ADD INDEX idx_analytics_events_ip_address (ip_address);
