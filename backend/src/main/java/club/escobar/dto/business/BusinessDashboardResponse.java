package club.escobar.dto.business;

import club.escobar.entity.enums.ApprovalStatus;

import java.math.BigDecimal;

public record BusinessDashboardResponse(
        ApprovalStatus approvalStatus,
        long totalCampaigns,
        long liveCampaigns,
        long campaignsPendingApproval,
        long contentAwaitingReview,
        long contentChangesRequested,
        long contentPendingLinkReview,
        long publishedContentCount,
        long payoutsPayableCount,
        BigDecimal payoutsPayableAmountInr,
        long payoutsPendingKycCount,
        BigDecimal totalPaidOutInr
) {
}
