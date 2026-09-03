package club.escobar.dto.creator;

import java.math.BigDecimal;

public record ActiveCampaignSummary(
        Long campaignId,
        String title,
        String status,
        long views,
        BigDecimal earningsInr
) {
}
