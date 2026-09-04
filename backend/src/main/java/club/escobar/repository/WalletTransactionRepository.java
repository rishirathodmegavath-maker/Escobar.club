package club.escobar.repository;

import club.escobar.entity.WalletTransaction;
import club.escobar.entity.enums.FundingSource;
import club.escobar.entity.enums.WalletTransactionStatus;
import club.escobar.entity.enums.WalletTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    @Query("""
            select coalesce(sum(t.amountInr), 0) from WalletTransaction t
            where t.business.id = :businessId and t.type = :type and t.status = :status
            """)
    BigDecimal sumAmountInrByBusiness_IdAndTypeAndStatus(@Param("businessId") Long businessId,
                                                           @Param("type") WalletTransactionType type,
                                                           @Param("status") WalletTransactionStatus status);

    @Query("""
            select coalesce(sum(t.amountInr), 0) from WalletTransaction t
            where t.business.id = :businessId and t.type = :type and t.status = :status
            and t.fundingSource in :sources
            """)
    BigDecimal sumAmountInrByBusiness_IdAndTypeAndStatusAndFundingSourceIn(@Param("businessId") Long businessId,
                                                                            @Param("type") WalletTransactionType type,
                                                                            @Param("status") WalletTransactionStatus status,
                                                                            @Param("sources") Collection<FundingSource> sources);

    // Global (cross-business) equivalents of the two sums above - back the admin financial
    // overview's "Total Available" (raw credit-minus-debit, every source, mirrors an individual
    // wallet's spendable balance) vs "Total Funds Held"/"Total Paid" (manual top-ups and campaign
    // payouts only, so a REVERSAL correction doesn't read as a fresh top-up or payout).
    @Query("select coalesce(sum(t.amountInr), 0) from WalletTransaction t where t.type = :type and t.status = :status")
    BigDecimal sumAmountInrByTypeAndStatus(@Param("type") WalletTransactionType type, @Param("status") WalletTransactionStatus status);

    @Query("""
            select coalesce(sum(t.amountInr), 0) from WalletTransaction t
            where t.type = :type and t.status = :status and t.fundingSource in :sources
            """)
    BigDecimal sumAmountInrByTypeAndStatusAndFundingSourceIn(@Param("type") WalletTransactionType type,
                                                               @Param("status") WalletTransactionStatus status,
                                                               @Param("sources") Collection<FundingSource> sources);

    // Backs the admin "All Wallets" list - one grouped query for every business id on the current
    // page, rather than 4 sum queries per row (see WalletBalanceRow).
    @Query("""
            select t.business.id as businessId,
                   coalesce(sum(case when t.type = :creditType and t.status = :confirmedStatus
                                      then t.amountInr else 0 end), 0) as totalCreditAll,
                   coalesce(sum(case when t.type = :debitType and t.status = :confirmedStatus
                                      then t.amountInr else 0 end), 0) as totalDebitAll,
                   coalesce(sum(case when t.type = :creditType and t.status = :confirmedStatus
                                       and t.fundingSource in :manualSources
                                      then t.amountInr else 0 end), 0) as totalAddedManual,
                   coalesce(sum(case when t.type = :debitType and t.status = :confirmedStatus
                                       and t.fundingSource = :campaignSource
                                      then t.amountInr else 0 end), 0) as totalPaidCampaign,
                   max(t.createdAt) as lastActivityAt
            from WalletTransaction t
            where t.business.id in :businessIds
            group by t.business.id
            """)
    List<WalletBalanceRow> sumBalancesByBusinessIds(@Param("businessIds") Collection<Long> businessIds,
                                                      @Param("creditType") WalletTransactionType creditType,
                                                      @Param("debitType") WalletTransactionType debitType,
                                                      @Param("confirmedStatus") WalletTransactionStatus confirmedStatus,
                                                      @Param("manualSources") Collection<FundingSource> manualSources,
                                                      @Param("campaignSource") FundingSource campaignSource);

    @Query("""
            select t from WalletTransaction t
            where t.business.id = :businessId
            and (:type is null or t.type = :type)
            and (:status is null or t.status = :status)
            and (:from is null or t.createdAt >= :from)
            and (:to is null or t.createdAt <= :to)
            order by t.createdAt desc
            """)
    Page<WalletTransaction> searchForBusiness(@Param("businessId") Long businessId,
                                               @Param("type") WalletTransactionType type,
                                               @Param("status") WalletTransactionStatus status,
                                               @Param("from") Instant from,
                                               @Param("to") Instant to,
                                               Pageable pageable);

    @Query("""
            select t from WalletTransaction t
            where (:businessId is null or t.business.id = :businessId)
            and (:type is null or t.type = :type)
            and (:status is null or t.status = :status)
            and (:fundingSource is null or t.fundingSource = :fundingSource)
            and (:from is null or t.createdAt >= :from)
            and (:to is null or t.createdAt <= :to)
            and (:search is null or lower(t.business.businessProfile.companyName) like lower(concat('%', :search, '%')))
            order by t.createdAt desc
            """)
    Page<WalletTransaction> searchForAdmin(@Param("businessId") Long businessId,
                                            @Param("type") WalletTransactionType type,
                                            @Param("status") WalletTransactionStatus status,
                                            @Param("fundingSource") FundingSource fundingSource,
                                            @Param("from") Instant from,
                                            @Param("to") Instant to,
                                            @Param("search") String search,
                                            Pageable pageable);

    List<WalletTransaction> findTop5ByOrderByCreatedAtDesc();

    long countByStatus(WalletTransactionStatus status);

    Optional<WalletTransaction> findTopByBusiness_IdOrderByCreatedAtDesc(Long businessId);

    boolean existsByReversedTransaction_Id(Long transactionId);
}
