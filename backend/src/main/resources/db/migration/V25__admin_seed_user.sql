-- Seeds the first ADMIN account. The password hash below is a random, syntactically-valid bcrypt
-- shape that is not derived from any real password, so it can never be used to log in as-is - the
-- account must be claimed via the existing "Forgot password" flow before first use.
INSERT INTO users (email, password_hash, role, is_active, email_verified, created_at, updated_at)
VALUES ('admin@escobar.club', '$2a$10$3zwHum/dRKoqJQedv1kAzVPe.A7lYtMxoo6UvkXl.j01e4k9viOgW', 'ADMIN', true, true, NOW(), NOW());
