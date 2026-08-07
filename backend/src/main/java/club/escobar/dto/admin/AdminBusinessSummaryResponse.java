package club.escobar.dto.admin;

import club.escobar.entity.enums.ApprovalStatus;

import java.time.Instant;

public record AdminBusinessSummaryResponse(
        Long userId,
        String email,
        String companyName,
        String gstNumber,
        String contactPersonName,
        ApprovalStatus approvalStatus,
        Instant createdAt
) {
}
