package club.escobar.dto.creator;

import java.time.Instant;

public record RecentActivityItem(
        String message,
        Instant occurredAt
) {
}
