package club.escobar.dto.admin;

import club.escobar.dto.wallet.WalletTransactionResponse;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(
        long totalBrands,
        long totalCreators,
        long totalCampaigns,
        long pendingCampaignApprovals,
        long pendingCreatorKyc,
        BigDecimal totalFundsHeldInr,
        BigDecimal totalPaidInr,
        BigDecimal totalAvailableInr,
        long activeWalletsCount,
        long pendingTopUpsCount,
        List<WalletTransactionResponse> recentWalletActivity
) {
}
