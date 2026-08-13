-- Renames the seeded admin account's login email to the operator's real inbox and resets its
-- password to the same syntactically-valid, non-derivable placeholder hash used when the account
-- was first seeded (V25) - it cannot be used to log in as-is, so the account must be reclaimed via
-- the "Forgot password" flow before next use, exactly like the original seed. Scoped to the exact
-- row seeded by V25 so this is a no-op if that account was already renamed by other means.
UPDATE users
SET email = 'theescobarclub@gmail.com',
    email_verified = true,
    password_hash = '$2a$10$3zwHum/dRKoqJQedv1kAzVPe.A7lYtMxoo6UvkXl.j01e4k9viOgW',
    updated_at = NOW()
WHERE email = 'admin@escobar.club' AND role = 'ADMIN';
