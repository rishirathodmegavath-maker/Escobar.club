package club.escobar.repository;

import club.escobar.entity.Payout;
import club.escobar.entity.enums.PayoutStatus;
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

public interface PayoutRepository extends JpaRepository<Payout, Long> {

    Optional<Payout> findByContent_Id(Long contentId);

    Page<Payout> findByCreator_Id(Long creatorId, Pageable pageable);

    Page<Payout> findByCreator_IdAndStatus(Long creatorId, PayoutStatus status, Pageable pageable);

    Page<Payout> findByBusiness_Id(Long businessId, Pageable pageable);

    Page<Payout> findByBusiness_IdAndStatus(Long businessId, PayoutStatus status, Pageable pageable);

    long countByBusiness_IdAndStatus(Long businessId, PayoutStatus status);

    @Query("""
            select coalesce(sum(p.amountInr), 0) from Payout p
            where p.business.id = :businessId and p.status = :status
            """)
    BigDecimal sumAmountInrByBusiness_IdAndStatus(@Param("businessId") Long businessId, @Param("status") PayoutStatus status);

    // Used by the campaign budget cap check - "committed" spend is whatever isn't BELOW_THRESHOLD
    // (that status always carries a zero amount anyway), passed in by the caller so the definition
    // of "committed" lives in one place (ContentServiceImpl) rather than being duplicated here.
    @Query("""
            select coalesce(sum(p.amountInr), 0) from Payout p
            where p.campaign.id = :campaignId and p.status in :statuses
            """)
    BigDecimal sumAmountInrByCampaign_IdAndStatusIn(@Param("campaignId") Long campaignId, @Param("statuses") Collection<PayoutStatus> statuses);

    @Query("""
            select coalesce(sum(p.amountInr), 0) from Payout p
            where p.creator.id = :creatorId and p.status = :status
            """)
    BigDecimal sumAmountInrByCreator_IdAndStatus(@Param("creatorId") Long creatorId, @Param("status") PayoutStatus status);

    @Query("""
            select coalesce(sum(p.amountInr), 0) from Payout p
            where p.creator.id = :creatorId and p.status = :status and p.paidAt >= :since
            """)
    BigDecimal sumAmountInrByCreator_IdAndStatusAndPaidAtAfter(@Param("creatorId") Long creatorId,
                                                                 @Param("status") PayoutStatus status,
                                                                 @Param("since") Instant since);

    // Business-scoped equivalent of sumAmountInrByCampaign_IdAndStatusIn, for the dashboard's
    // portfolio-wide "Committed" total across all of a business's campaigns at once.
    @Query("""
            select coalesce(sum(p.amountInr), 0) from Payout p
            where p.business.id = :businessId and p.status in :statuses
            """)
    BigDecimal sumAmountInrByBusiness_IdAndStatusIn(@Param("businessId") Long businessId, @Param("statuses") Collection<PayoutStatus> statuses);

    // Backs the per-campaign "committed" figure in the Business Dashboard's campaign preview list -
    // one grouped query across all of a business's campaigns rather than one query per campaign.
    @Query("""
            select p.campaign.id as campaignId, coalesce(sum(p.amountInr), 0) as committed
            from Payout p
            where p.business.id = :businessId and p.status in :statuses
            group by p.campaign.id
            """)
    List<CampaignCommittedRow> sumCommittedByBusinessGroupedByCampaign(@Param("businessId") Long businessId,
                                                                        @Param("statuses") Collection<PayoutStatus> statuses);
}
