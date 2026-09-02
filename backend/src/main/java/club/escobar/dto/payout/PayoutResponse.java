package club.escobar.dto.payout;

import club.escobar.entity.enums.PayoutStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PayoutResponse(
        Long id,
        Long contentId,
        Long creatorId,
        String creatorDisplayName,
        Long campaignId,
        String campaignTitle,
        Long businessId,
        Long viewCountUsed,
        BigDecimal rateUsed,
        BigDecimal amountInr,
        PayoutStatus status,
        Instant calculatedAt,
        Instant eligibleAt,
        Instant paidAt,
        String paidNote
) {
}
