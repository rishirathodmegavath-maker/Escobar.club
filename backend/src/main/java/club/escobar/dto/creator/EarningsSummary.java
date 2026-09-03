package club.escobar.dto.creator;

import java.math.BigDecimal;

public record EarningsSummary(
        BigDecimal pendingKycInr,
        BigDecimal payableInr,
        BigDecimal paidInr,
        BigDecimal thisMonthPaidInr
) {
}
