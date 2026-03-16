-- noinspection SqlDialectInspectionForFile
-- noinspection SqlNoDataSourceInspectionForFile
CREATE TABLE IF NOT EXISTS clients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(30),
    lang CHAR(2) NOT NULL DEFAULT 'nl',
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_clients_email UNIQUE (email)
);

CREATE INDEX idx_clients_email ON clients (email);

CREATE TABLE IF NOT EXISTS client_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id BIGINT NOT NULL,
    token CHAR(64) NOT NULL,
    type VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_client_tokens_token UNIQUE (token),
    CONSTRAINT fk_client_tokens_client FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE CASCADE
);

CREATE INDEX idx_client_tokens_token ON client_tokens (token);
CREATE INDEX idx_client_tokens_expires ON client_tokens (expires_at);

CREATE TABLE IF NOT EXISTS appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    type VARCHAR(30) NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    duration_min SMALLINT DEFAULT 60,
    location VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'scheduled',
    notes_coach TEXT,
    notes_shared TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appointments_client FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE CASCADE
);

CREATE INDEX idx_appointments_client ON appointments (client_id);
CREATE INDEX idx_appointments_scheduled ON appointments (scheduled_at);

CREATE TABLE IF NOT EXISTS training_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_training_plans_client FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE CASCADE
);

CREATE INDEX idx_training_plans_client ON training_plans (client_id);

CREATE TABLE IF NOT EXISTS training_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    sort_order SMALLINT DEFAULT 0,
    category VARCHAR(20) NOT NULL DEFAULT 'exercise',
    title VARCHAR(200) NOT NULL,
    description TEXT,
    sets TINYINT,
    reps TINYINT,
    duration_sec INT,
    completed_by_client BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_training_items_plan FOREIGN KEY (plan_id) REFERENCES training_plans (id) ON DELETE CASCADE
);

CREATE INDEX idx_training_items_plan ON training_items (plan_id);

CREATE TABLE IF NOT EXISTS invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id BIGINT NOT NULL,
    invoice_number VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    amount_cents INT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'EUR',
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    due_date DATE,
    paid_at TIMESTAMP NULL,
    payment_method VARCHAR(100),
    pdf_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_invoices_number UNIQUE (invoice_number),
    CONSTRAINT fk_invoices_client FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE CASCADE
);

CREATE INDEX idx_invoices_client ON invoices (client_id);
CREATE INDEX idx_invoices_status ON invoices (status);

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id BIGINT NOT NULL,
    sender VARCHAR(10) NOT NULL,
    body TEXT NOT NULL,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_messages_client FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE CASCADE
);

CREATE INDEX idx_messages_client ON messages (client_id);
CREATE INDEX idx_messages_created ON messages (created_at);
