CREATE TABLE IF NOT EXISTS contact_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    email VARCHAR(120) NOT NULL,
    request_type VARCHAR(20),
    phone VARCHAR(40),
    topic VARCHAR(20),
    preferred_time VARCHAR(120),
    goal TEXT,
    message TEXT,
    page VARCHAR(60),
    lang VARCHAR(10),
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    referrer VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_contact_requests_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS page_views (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    path VARCHAR(200) NOT NULL,
    title VARCHAR(200),
    referrer VARCHAR(255),
    lang VARCHAR(10),
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_page_views_created_at (created_at)
);

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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_analytics_events_created_at (created_at),
    INDEX idx_analytics_events_type_name (event_type, event_name)
);
