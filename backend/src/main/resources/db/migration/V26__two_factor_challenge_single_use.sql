-- The 2FA login-challenge JWT was previously purely stateless (5-minute expiry, no server-side
-- tracking), meaning it could be replayed to mint a fresh session repeatedly until it expired.
-- This table records each challenge token's unique jti the moment it's redeemed, so a second
-- redemption attempt with the same token is rejected even though the JWT itself is still valid.
CREATE TABLE used_two_factor_challenges (
    jti         VARCHAR(64) NOT NULL PRIMARY KEY,
    expires_at  TIMESTAMP NOT NULL,
    used_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE INDEX idx_used_two_factor_challenges_expires ON used_two_factor_challenges (expires_at);
