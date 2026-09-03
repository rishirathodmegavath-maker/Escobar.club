package club.escobar.dto.business;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CampaignPreview(
        Long campaignId,
        String title,
        String status,
        long creatorsCount,
        long contentSubmittedCount,
        long contentPublishedCount,
        long views,
        BigDecimal maxBudgetInr,
        BigDecimal committedBudgetInr,
        LocalDate submissionDeadline
) {
}
