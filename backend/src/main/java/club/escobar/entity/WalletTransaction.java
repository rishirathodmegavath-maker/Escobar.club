package club.escobar.entity;

import club.escobar.entity.enums.FundingSource;
import club.escobar.entity.enums.WalletTransactionStatus;
import club.escobar.entity.enums.WalletTransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

// Append-only ledger row. amount_inr/type/business/fundingSource are never mutated once created -
// only `status` (and the confirmedAt/confirmedBy pair) ever changes in place, exactly like
// Payout.status transitions BELOW_THRESHOLD -> PENDING_KYC/PAYABLE -> PAID. A correction is never
// an edit; it's a brand new row (see WalletServiceImpl.reverseTransaction).
@Entity
@Table(name = "wallet_transactions", indexes = {
        @Index(name = "idx_wallet_tx_business", columnList = "business_id"),
        @Index(name = "idx_wallet_tx_business_status", columnList = "business_id, status"),
        @Index(name = "idx_wallet_tx_status", columnList = "status"),
        @Index(name = "idx_wallet_tx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private User business;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WalletTransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WalletTransactionStatus status = WalletTransactionStatus.CONFIRMED;

    @Enumerated(EnumType.STRING)
    @Column(name = "funding_source", nullable = false, length = 30)
    private FundingSource fundingSource;

    @Column(name = "amount_inr", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountInr;

    @Column(length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performed_by_user_id", nullable = false)
    private User performedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "payout_id")
    private Payout payout;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "reversed_transaction_id")
    private WalletTransaction reversedTransaction;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "confirmed_by_user_id")
    private User confirmedBy;
}
