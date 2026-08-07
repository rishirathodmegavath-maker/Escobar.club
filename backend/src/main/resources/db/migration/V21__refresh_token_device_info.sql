ALTER TABLE refresh_tokens
    ADD COLUMN ip_address VARCHAR(64) NULL;

ALTER TABLE refresh_tokens
    ADD COLUMN user_agent VARCHAR(255) NULL;

ALTER TABLE refresh_tokens
    ADD COLUMN last_used_at TIMESTAMP NULL;
