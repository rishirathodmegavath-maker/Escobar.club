package club.escobar.dto.wallet;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletSummaryResponse(
        Long businessId,
        String businessName,
        BigDecimal availableBalanceInr,
        BigDecimal totalAddedInr,
        BigDecimal totalPaidInr,
        Instant lastActivityAt
) {
}
