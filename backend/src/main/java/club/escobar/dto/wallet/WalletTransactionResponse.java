package club.escobar.dto.wallet;

import club.escobar.entity.enums.FundingSource;
import club.escobar.entity.enums.WalletTransactionStatus;
import club.escobar.entity.enums.WalletTransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletTransactionResponse(
        Long id,
        Long businessId,
        String businessName,
        WalletTransactionType type,
        WalletTransactionStatus status,
        FundingSource fundingSource,
        BigDecimal amountInr,
        String note,
        Long performedByUserId,
        String performedByName,
        Long payoutId,
        String campaignTitle,
        Long reversedTransactionId,
        Instant createdAt,
        Instant confirmedAt,
        String confirmedByName
) {
}
