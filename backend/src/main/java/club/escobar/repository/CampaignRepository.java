package club.escobar.repository;

import club.escobar.entity.Campaign;
import club.escobar.entity.enums.ApprovalStatus;
import club.escobar.entity.enums.CampaignStatus;
import club.escobar.entity.enums.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    Page<Campaign> findByBusiness_Id(Long businessId, Pageable pageable);

    long countByBusiness_Id(Long businessId);

    long countByBusiness_IdAndApprovalStatus(Long businessId, ApprovalStatus approvalStatus);

    long countByApprovalStatus(ApprovalStatus status);

    // Mirrors Campaign.getEffectiveStatus()'s LIVE window (PUBLISHED and today within the publish dates).
    @Query("""
            select count(c) from Campaign c
            where c.business.id = :businessId
            and c.status = club.escobar.entity.enums.CampaignStatus.PUBLISHED
            and c.publishStartAt <= CURRENT_DATE
            and c.publishEndAt >= CURRENT_DATE
            """)
    long countLiveByBusinessId(@Param("businessId") Long businessId);

    // "Near cap" = committed (PENDING_KYC+PAYABLE+PAID) payouts have reached at least
    // thresholdFraction of the campaign's budget cap - an early warning before submissions
    // actually auto-close. Campaigns with no cap set (unlimited) never match.
    @Query("""
            select count(c) from Campaign c
            where c.business.id = :businessId
            and c.maxBudgetInr is not null
            and coalesce((select sum(p.amountInr) from Payout p
                          where p.campaign = c and p.status in :statuses), 0)
                >= c.maxBudgetInr * :thresholdFraction
            """)
    long countNearBudgetCapByBusinessId(@Param("businessId") Long businessId,
                                         @Param("statuses") Collection<PayoutStatus> statuses,
                                         @Param("thresholdFraction") BigDecimal thresholdFraction);

    @Query("""
            select c from Campaign c
            where c.status <> club.escobar.entity.enums.CampaignStatus.DRAFT
            and c.status <> club.escobar.entity.enums.CampaignStatus.CANCELLED
            and c.approvalStatus = club.escobar.entity.enums.ApprovalStatus.APPROVED
            and (:search is null or lower(c.title) like lower(concat('%', :search, '%')))
            """)
    Page<Campaign> searchPublic(@Param("search") String search, Pageable pageable);

    @Query("""
            select c from Campaign c
            where c.status = :status
            and c.approvalStatus = club.escobar.entity.enums.ApprovalStatus.APPROVED
            and (:search is null or lower(c.title) like lower(concat('%', :search, '%')))
            """)
    Page<Campaign> searchPublicByStatus(@Param("search") String search, @Param("status") CampaignStatus status, Pageable pageable);

    @Query("""
            select c from Campaign c
            where (:status is null or c.approvalStatus = :status)
            and (:search is null or lower(c.title) like lower(concat('%', :search, '%')))
            """)
    Page<Campaign> searchForAdmin(@Param("search") String search, @Param("status") ApprovalStatus status, Pageable pageable);

    // Rates of campaigns currently open for creator submissions, used to compute the "Hot" rate threshold.
    @Query("""
            select c.ratePerThousandViewsInr from Campaign c
            where c.status = club.escobar.entity.enums.CampaignStatus.PUBLISHED
            and c.submissionOpenAt <= CURRENT_DATE and c.submissionDeadline >= CURRENT_DATE
            and CURRENT_DATE < c.publishStartAt
            """)
    List<BigDecimal> findActiveRates();

    // Every campaign this creator has ever submitted content against, regardless of that content's
    // status - used to build "My Active Campaigns" (effective status filtering happens in the
    // service layer via the entity's own getEffectiveStatus(), not duplicated here).
    @Query("""
            select distinct camp from Content ct join ct.campaign camp
            where ct.creator.id = :creatorId
            """)
    List<Campaign> findDistinctCampaignsByCreatorId(@Param("creatorId") Long creatorId);

    // Campaigns open for submission that this creator hasn't already submitted content against,
    // excluding any that have already hit their budget cap (mirrors the committed-spend subquery
    // in countNearBudgetCapByBusinessId). Backs "Recommended Campaigns" - see
    // CampaignServiceImpl.listRecommendedForCreator for why this isn't true niche matching.
    @Query("""
            select c from Campaign c
            where c.status = club.escobar.entity.enums.CampaignStatus.PUBLISHED
            and c.approvalStatus = club.escobar.entity.enums.ApprovalStatus.APPROVED
            and c.submissionOpenAt <= CURRENT_DATE and c.submissionDeadline >= CURRENT_DATE
            and CURRENT_DATE < c.publishStartAt
            and c.id not in (select ct.campaign.id from Content ct where ct.creator.id = :creatorId)
            and (c.maxBudgetInr is null or coalesce((select sum(p.amountInr) from Payout p
                          where p.campaign = c and p.status in :statuses), 0) < c.maxBudgetInr)
            order by c.urgent desc, c.submissionDeadline asc
            """)
    List<Campaign> findOpenNotYetSubmittedByCreator(@Param("creatorId") Long creatorId,
                                                      @Param("statuses") Collection<PayoutStatus> statuses,
                                                      Pageable pageable);

    // Same "near cap" predicate as countNearBudgetCapByBusinessId, but returning the campaigns
    // themselves for the Needs Attention detail list instead of just a count.
    @Query("""
            select c from Campaign c
            where c.business.id = :businessId
            and c.maxBudgetInr is not null
            and coalesce((select sum(p.amountInr) from Payout p
                          where p.campaign = c and p.status in :statuses), 0)
                >= c.maxBudgetInr * :thresholdFraction
            order by c.updatedAt desc
            """)
    List<Campaign> findNearBudgetCapByBusinessId(@Param("businessId") Long businessId,
                                                  @Param("statuses") Collection<PayoutStatus> statuses,
                                                  @Param("thresholdFraction") BigDecimal thresholdFraction,
                                                  Pageable pageable);

    @Query("""
            select coalesce(sum(c.maxBudgetInr), 0) from Campaign c
            where c.business.id = :businessId and c.maxBudgetInr is not null
            """)
    BigDecimal sumMaxBudgetInrByBusinessId(@Param("businessId") Long businessId);
}
