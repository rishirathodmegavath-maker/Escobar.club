package club.escobar.dto.campaign;

import java.math.BigDecimal;

public record CampaignAnalyticsResponse(
        Long campaignId,
        long views,
        long likes,
        long comments,
        double engagementRate,
        long creatorsCount,
        long publishedContentCount,
        BigDecimal budgetCommittedInr
) {
}
