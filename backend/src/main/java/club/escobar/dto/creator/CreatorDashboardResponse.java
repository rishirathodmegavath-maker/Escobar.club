package club.escobar.dto.creator;

import club.escobar.dto.campaign.CampaignResponse;
import club.escobar.dto.common.NeedsAttentionItem;
import club.escobar.dto.common.PerformanceSummary;
import club.escobar.dto.common.TopContentItem;
import club.escobar.entity.enums.KycStatus;

import java.util.List;

public record CreatorDashboardResponse(
        // Null means the creator has never submitted a KYC application yet.
        KycStatus kycStatus,
        int profileCompletionPercent,
        List<String> profileCompletionMissing,
        long activeCampaignsCount,
        ContentStatusCounts submissionStatus,
        EarningsSummary earnings,
        PerformanceSummary performance,
        List<NeedsAttentionItem> needsAttention,
        List<CampaignResponse> recommendedCampaigns,
        List<ActiveCampaignSummary> activeCampaigns,
        List<TopContentItem> topContent,
        List<RecentActivityItem> recentActivity,
        List<PayoutPreviewItem> recentPayouts
) {
}
