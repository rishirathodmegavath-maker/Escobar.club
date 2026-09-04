package club.escobar.entity.enums;

// A reversed row is never mutated to this status in place - WalletServiceImpl.reverseTransaction
// preserves the original untouched (still CONFIRMED, still counted) and nets it out with a brand
// new offsetting row instead, so the ledger keeps a visible "REVERSAL" line item rather than
// silently rewriting history. REVERSED is kept in the vocabulary for API/UI completeness but isn't
// currently assigned by any code path.
public enum WalletTransactionStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    REVERSED
}
