ALTER TABLE contact_requests
    ADD COLUMN IF NOT EXISTS request_type VARCHAR(20);

ALTER TABLE contact_requests
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'new';

ALTER TABLE contact_requests
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE contact_requests
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

UPDATE contact_requests
SET status = 'new'
WHERE status IS NULL OR status = '';

UPDATE contact_requests
SET updated_at = created_at
WHERE updated_at IS NULL;
