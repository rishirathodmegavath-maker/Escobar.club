package club.escobar.dto.business;

import club.escobar.dto.common.NeedsAttentionItem;
import club.escobar.dto.common.PerformanceSummary;
import club.escobar.dto.common.TopContentItem;
import club.escobar.entity.enums.ApprovalStatus;

import java.math.BigDecimal;
import java.util.List;

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
        BigDecimal totalPaidOutInr,
        long campaignsNearBudgetCap,
        long approvedContentCount,
        long rejectedContentCount,
        long participatingCreatorsCount,
        long totalViews,
        long totalEngagement,
        BigDecimal totalBudgetInr,
        BigDecimal totalCommittedInr,
        BigDecimal totalRemainingInr,
        List<NeedsAttentionItem> needsAttention,
        List<CampaignPreview> campaignsPreview,
        List<CreatorActivityItem> creatorActivity,
        List<TopContentItem> topContent,
        PerformanceSummary performance
) {
}
