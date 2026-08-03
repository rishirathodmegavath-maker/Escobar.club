ALTER TABLE users
    MODIFY password_hash VARCHAR(255) NULL;

ALTER TABLE users
    ADD COLUMN google_id VARCHAR(255) NULL,
    ADD CONSTRAINT uk_users_google_id UNIQUE (google_id);
