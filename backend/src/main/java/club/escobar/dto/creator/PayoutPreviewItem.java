package club.escobar.dto.creator;

import club.escobar.entity.enums.PayoutStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PayoutPreviewItem(
        Long contentId,
        String campaignTitle,
        BigDecimal amountInr,
        PayoutStatus status,
        Instant paidAt
) {
}
