package club.escobar.dto.kyc;

import club.escobar.entity.enums.KycStatus;

import java.time.Instant;

public record CreatorKycProfileResponse(
        Long creatorId,
        String panNumberMasked,
        String nameOnPan,
        boolean hasDocument,
        KycStatus status,
        String reviewNote,
        Instant reviewedAt
) {
}
