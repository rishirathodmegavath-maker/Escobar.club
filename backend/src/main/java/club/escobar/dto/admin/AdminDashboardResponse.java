package club.escobar.dto.admin;

public record AdminDashboardResponse(
        long totalBrands,
        long totalCreators,
        long totalCampaigns,
        long pendingCampaignApprovals,
        long pendingCreatorKyc
) {
}
