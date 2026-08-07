package club.escobar.security;

import club.escobar.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final long TWO_FACTOR_CHALLENGE_TTL_SECONDS = 5 * 60;

    private final JwtProperties jwtProperties;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(SecurityUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claims(Map.of(
                        "uid", user.getId(),
                        "role", user.getRole(),
                        "type", "access"
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtProperties.accessTokenTtlMinutes() * 60)))
                .signWith(signingKey())
                .compact();
    }

    public String newRefreshTokenValue() {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("refresh")
                .claims(Map.of("type", "refresh"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtProperties.refreshTokenTtlDays() * 24 * 3600)))
                .id(java.util.UUID.randomUUID().toString())
                .signWith(signingKey())
                .compact();
    }

    public Instant refreshTokenExpiry() {
        return Instant.now().plusSeconds(jwtProperties.refreshTokenTtlDays() * 24 * 3600);
    }

    /**
     * Short-lived token proving the password step already succeeded; only redeemable once via
     * /auth/2fa/verify. Carries a unique jti so the caller can record it as consumed on first
     * successful redemption - the JWT itself stays cryptographically valid until it expires, but a
     * second redemption attempt with the same jti must be rejected.
     */
    public String generateTwoFactorChallengeToken(SecurityUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .id(java.util.UUID.randomUUID().toString())
                .claims(Map.of("uid", user.getId(), "type", "2fa_challenge"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(TWO_FACTOR_CHALLENGE_TTL_SECONDS)))
                .signWith(signingKey())
                .compact();
    }

    public boolean isTwoFactorChallengeToken(String token) {
        try {
            return "2fa_challenge".equals(parseClaims(token).get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
