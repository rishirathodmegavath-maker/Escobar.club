package club.escobar.dto.admin;

import club.escobar.entity.enums.KycStatus;

import java.time.Instant;

public record AdminCreatorSummaryResponse(
        Long userId,
        String email,
        String displayName,
        boolean active,
        KycStatus kycStatus,
        Instant createdAt
) {
}
