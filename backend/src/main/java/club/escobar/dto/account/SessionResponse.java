package club.escobar.dto.account;

import java.time.Instant;

public record SessionResponse(
        Long id,
        String ipAddress,
        String userAgent,
        Instant createdAt,
        Instant lastUsedAt,
        Instant expiresAt
) {
}
