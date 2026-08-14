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
        Instant reviewedAt,
        // True only when status is VERIFIED and the verification was authored by an admin - a
        // business's own peer review of a creator's KYC does not unlock campaign participation
        // platform-wide (same rule ContentServiceImpl/PayoutServiceImpl already enforce).
        boolean eligibleToParticipate
) {
}
