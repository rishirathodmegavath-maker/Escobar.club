package club.escobar.dto.business;

import java.time.Instant;

public record CreatorActivityItem(
        String creatorDisplayName,
        String campaignTitle,
        String message,
        Instant occurredAt
) {
}
