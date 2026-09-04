package club.escobar.repository;

import java.math.BigDecimal;
import java.time.Instant;

// Backs the admin "All Wallets" list - one grouped query across every business on the page rather
// than one sum query per business. totalCreditAll/totalDebitAll (all funding sources, CONFIRMED
// only) net out to the spendable balance; totalAddedManual/totalPaidCampaign are the narrower,
// human-facing "Total Added"/"Total Paid" figures (manual top-ups and campaign payouts only - a
// REVERSAL correction still affects the balance but isn't counted as a fresh top-up or payout).
public interface WalletBalanceRow {
    Long getBusinessId();

    BigDecimal getTotalCreditAll();

    BigDecimal getTotalDebitAll();

    BigDecimal getTotalAddedManual();

    BigDecimal getTotalPaidCampaign();

    Instant getLastActivityAt();
}
