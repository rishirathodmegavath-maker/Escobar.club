package club.escobar.repository;

import club.escobar.entity.Content;
import club.escobar.entity.enums.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ContentRepository extends JpaRepository<Content, Long> {

    Page<Content> findByBusiness_Id(Long businessId, Pageable pageable);

    Page<Content> findByBusiness_IdAndStatus(Long businessId, ContentStatus status, Pageable pageable);

    long countByBusiness_IdAndStatus(Long businessId, ContentStatus status);

    Page<Content> findByCreator_Id(Long creatorId, Pageable pageable);

    long countByCreator_IdAndStatus(Long creatorId, ContentStatus status);

    List<Content> findByCreator_IdAndStatus(Long creatorId, ContentStatus status);

    @Query("select count(distinct c.creator.id) from Content c where c.business.id = :businessId")
    long countDistinctCreatorsByBusinessId(@Param("businessId") Long businessId);

    Page<Content> findByStatus(ContentStatus status, Pageable pageable);

    boolean existsByCreator_IdAndBusiness_Id(Long creatorId, Long businessId);

    boolean existsByCampaign_Id(Long campaignId);

    // "Due for sync" = no metrics snapshot at all, or the latest one is older than the cutoff (the
    // caller passes now-minus-minIntervalMinutes). Used by the scheduled refresh job so it never
    // re-fetches content whose data is already fresh - the rate limit lives in this query, not in
    // per-call bookkeeping.
    @Query("""
            SELECT c FROM Content c
            WHERE c.status = :status
            AND NOT EXISTS (
                SELECT 1 FROM ContentMetricsSnapshot s
                WHERE s.content = c AND s.fetchedAt > :cutoff
            )
            """)
    Page<Content> findDueForMetricsSync(@Param("status") ContentStatus status, @Param("cutoff") Instant cutoff, Pageable pageable);

    // Ranks creators by the view count of the LATEST metrics snapshot per published content item
    // (not summed history), scoped to one business. The ROW_NUMBER() window function picks the
    // latest snapshot per content before the outer aggregation sums across a creator's content.
    @Query(value = """
            SELECT c.creator_id AS creatorId,
                   cp.display_name AS creatorDisplayName,
                   cp.profile_picture_url AS creatorProfilePictureUrl,
                   SUM(COALESCE(latest.view_count, 0)) AS totalViews,
                   COUNT(DISTINCT c.id) AS publishedContentCount
            FROM content c
            LEFT JOIN (
                SELECT cms.content_id, cms.view_count,
                       ROW_NUMBER() OVER (PARTITION BY cms.content_id ORDER BY cms.fetched_at DESC) AS rn
                FROM content_metrics_snapshots cms
            ) latest ON latest.content_id = c.id AND latest.rn = 1
            JOIN creator_profiles cp ON cp.id = c.creator_id
            WHERE c.status = 'PUBLISHED' AND c.business_id = :businessId
            GROUP BY c.creator_id, cp.display_name, cp.profile_picture_url
            ORDER BY totalViews DESC, c.creator_id ASC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT c.creator_id)
            FROM content c
            WHERE c.status = 'PUBLISHED' AND c.business_id = :businessId
            """,
            nativeQuery = true)
    Page<LeaderboardRow> findBusinessLeaderboard(@Param("businessId") Long businessId, Pageable pageable);

    // Same ranking as findBusinessLeaderboard but across all businesses platform-wide.
    @Query(value = """
            SELECT c.creator_id AS creatorId,
                   cp.display_name AS creatorDisplayName,
                   cp.profile_picture_url AS creatorProfilePictureUrl,
                   SUM(COALESCE(latest.view_count, 0)) AS totalViews,
                   COUNT(DISTINCT c.id) AS publishedContentCount
            FROM content c
            LEFT JOIN (
                SELECT cms.content_id, cms.view_count,
                       ROW_NUMBER() OVER (PARTITION BY cms.content_id ORDER BY cms.fetched_at DESC) AS rn
                FROM content_metrics_snapshots cms
            ) latest ON latest.content_id = c.id AND latest.rn = 1
            JOIN creator_profiles cp ON cp.id = c.creator_id
            WHERE c.status = 'PUBLISHED'
            GROUP BY c.creator_id, cp.display_name, cp.profile_picture_url
            ORDER BY totalViews DESC, c.creator_id ASC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT c.creator_id)
            FROM content c
            WHERE c.status = 'PUBLISHED'
            """,
            nativeQuery = true)
    Page<LeaderboardRow> findGlobalLeaderboard(Pageable pageable);

    // Same latest-snapshot-per-content pattern as the leaderboard queries above, but ranked by
    // individual content item (not summed per creator) and scoped to one creator. Pageable's limit
    // is applied directly to this native query without a countQuery since the return type is a
    // plain List, not a Page - callers pass PageRequest.of(0, N) purely to cap the result size.
    @Query(value = """
            SELECT c.id AS contentId,
                   cp.display_name AS creatorDisplayName,
                   camp.title AS campaignTitle,
                   c.media_type AS mediaType,
                   COALESCE(latest.view_count, 0) AS views,
                   COALESCE(latest.like_count, 0) AS likes,
                   COALESCE(latest.comment_count, 0) AS comments,
                   c.post_url AS postUrl
            FROM content c
            JOIN campaigns camp ON camp.id = c.campaign_id
            JOIN creator_profiles cp ON cp.id = c.creator_id
            LEFT JOIN (
                SELECT cms.content_id, cms.view_count, cms.like_count, cms.comment_count,
                       ROW_NUMBER() OVER (PARTITION BY cms.content_id ORDER BY cms.fetched_at DESC) AS rn
                FROM content_metrics_snapshots cms
            ) latest ON latest.content_id = c.id AND latest.rn = 1
            WHERE c.status = 'PUBLISHED' AND c.creator_id = :creatorId
            ORDER BY views DESC, c.id ASC
            """,
            nativeQuery = true)
    List<TopContentRow> findTopContentByCreator(@Param("creatorId") Long creatorId, Pageable pageable);

    // Business-scoped equivalent of findTopContentByCreator.
    @Query(value = """
            SELECT c.id AS contentId,
                   cp.display_name AS creatorDisplayName,
                   camp.title AS campaignTitle,
                   c.media_type AS mediaType,
                   COALESCE(latest.view_count, 0) AS views,
                   COALESCE(latest.like_count, 0) AS likes,
                   COALESCE(latest.comment_count, 0) AS comments,
                   c.post_url AS postUrl
            FROM content c
            JOIN campaigns camp ON camp.id = c.campaign_id
            JOIN creator_profiles cp ON cp.id = c.creator_id
            LEFT JOIN (
                SELECT cms.content_id, cms.view_count, cms.like_count, cms.comment_count,
                       ROW_NUMBER() OVER (PARTITION BY cms.content_id ORDER BY cms.fetched_at DESC) AS rn
                FROM content_metrics_snapshots cms
            ) latest ON latest.content_id = c.id AND latest.rn = 1
            WHERE c.status = 'PUBLISHED' AND c.business_id = :businessId
            ORDER BY views DESC, c.id ASC
            """,
            nativeQuery = true)
    List<TopContentRow> findTopContentByBusiness(@Param("businessId") Long businessId, Pageable pageable);

    // Sums the latest-snapshot views/likes/comments for a creator's PUBLISHED content published on
    // or after `since` (callers pass Instant.EPOCH for "all time" rather than a nullable parameter).
    // A bare aggregate with no GROUP BY always returns exactly one row, even when zero content
    // matches, so the caller never has to null-check the projection itself.
    @Query(value = """
            SELECT COALESCE(SUM(latest.view_count), 0) AS views,
                   COALESCE(SUM(latest.like_count), 0) AS likes,
                   COALESCE(SUM(latest.comment_count), 0) AS comments,
                   COUNT(DISTINCT c.id) AS publishedCount
            FROM content c
            LEFT JOIN (
                SELECT cms.content_id, cms.view_count, cms.like_count, cms.comment_count,
                       ROW_NUMBER() OVER (PARTITION BY cms.content_id ORDER BY cms.fetched_at DESC) AS rn
                FROM content_metrics_snapshots cms
            ) latest ON latest.content_id = c.id AND latest.rn = 1
            WHERE c.status = 'PUBLISHED' AND c.creator_id = :creatorId AND c.published_at >= :since
            """,
            nativeQuery = true)
    MetricsRollupRow sumMetricsByCreatorAndPublishedAtAfter(@Param("creatorId") Long creatorId, @Param("since") Instant since);

    // Business-scoped equivalent of sumMetricsByCreatorAndPublishedAtAfter.
    @Query(value = """
            SELECT COALESCE(SUM(latest.view_count), 0) AS views,
                   COALESCE(SUM(latest.like_count), 0) AS likes,
                   COALESCE(SUM(latest.comment_count), 0) AS comments,
                   COUNT(DISTINCT c.id) AS publishedCount
            FROM content c
            LEFT JOIN (
                SELECT cms.content_id, cms.view_count, cms.like_count, cms.comment_count,
                       ROW_NUMBER() OVER (PARTITION BY cms.content_id ORDER BY cms.fetched_at DESC) AS rn
                FROM content_metrics_snapshots cms
            ) latest ON latest.content_id = c.id AND latest.rn = 1
            WHERE c.status = 'PUBLISHED' AND c.business_id = :businessId AND c.published_at >= :since
            """,
            nativeQuery = true)
    MetricsRollupRow sumMetricsByBusinessAndPublishedAtAfter(@Param("businessId") Long businessId, @Param("since") Instant since);

    // Aggregates a creator's views (latest snapshot) and total payout earnings per campaign, in one
    // query rather than looping per-campaign - paired with findDistinctCampaignsByCreatorId in
    // CampaignRepository to build "My Active Campaigns" without N+1 queries.
    @Query(value = """
            SELECT c.campaign_id AS campaignId,
                   COALESCE(SUM(latest.view_count), 0) AS views,
                   COALESCE(SUM(p.amount_inr), 0) AS earnings
            FROM content c
            LEFT JOIN (
                SELECT cms.content_id, cms.view_count,
                       ROW_NUMBER() OVER (PARTITION BY cms.content_id ORDER BY cms.fetched_at DESC) AS rn
                FROM content_metrics_snapshots cms
            ) latest ON latest.content_id = c.id AND latest.rn = 1
            LEFT JOIN payouts p ON p.content_id = c.id
            WHERE c.creator_id = :creatorId
            GROUP BY c.campaign_id
            """,
            nativeQuery = true)
    List<CampaignAggregateRow> sumCampaignAggregatesByCreatorId(@Param("creatorId") Long creatorId);

    // Backs GET /api/campaigns/{id}/analytics - a single campaign's PUBLISHED-content metrics,
    // computed on demand rather than folded into the business dashboard's aggregate payload (a
    // business could have dozens of campaigns; this only runs when one is actually selected).
    @Query(value = """
            SELECT COALESCE(SUM(latest.view_count), 0) AS views,
                   COALESCE(SUM(latest.like_count), 0) AS likes,
                   COALESCE(SUM(latest.comment_count), 0) AS comments,
                   COUNT(DISTINCT c.creator_id) AS creatorsCount,
                   COUNT(DISTINCT c.id) AS publishedContentCount
            FROM content c
            LEFT JOIN (
                SELECT cms.content_id, cms.view_count, cms.like_count, cms.comment_count,
                       ROW_NUMBER() OVER (PARTITION BY cms.content_id ORDER BY cms.fetched_at DESC) AS rn
                FROM content_metrics_snapshots cms
            ) latest ON latest.content_id = c.id AND latest.rn = 1
            WHERE c.status = 'PUBLISHED' AND c.campaign_id = :campaignId
            """,
            nativeQuery = true)
    CampaignMetricsRow findCampaignMetrics(@Param("campaignId") Long campaignId);

    // Per-campaign rollup for the Business Dashboard's "My Campaigns" preview - one grouped query
    // across all of a business's campaigns rather than one query per campaign.
    @Query(value = """
            SELECT c.campaign_id AS campaignId,
                   COUNT(DISTINCT c.creator_id) AS creatorsCount,
                   COUNT(DISTINCT c.id) AS contentSubmittedCount,
                   COUNT(DISTINCT CASE WHEN c.status = 'PUBLISHED' THEN c.id END) AS contentPublishedCount,
                   COALESCE(SUM(CASE WHEN c.status = 'PUBLISHED' THEN latest.view_count ELSE 0 END), 0) AS views
            FROM content c
            LEFT JOIN (
                SELECT cms.content_id, cms.view_count,
                       ROW_NUMBER() OVER (PARTITION BY cms.content_id ORDER BY cms.fetched_at DESC) AS rn
                FROM content_metrics_snapshots cms
            ) latest ON latest.content_id = c.id AND latest.rn = 1
            WHERE c.business_id = :businessId
            GROUP BY c.campaign_id
            """,
            nativeQuery = true)
    List<BusinessCampaignPreviewRow> findCampaignPreviewAggregatesByBusinessId(@Param("businessId") Long businessId);

    // Backs the "Total Views" counter on Business > My Campaigns: latest-snapshot views summed over
    // PUBLISHED content only, grouped by campaign - scoped to the campaign ids on the current page
    // (not a business's whole history) so listing campaigns never has to aggregate more than it's
    // about to display. Same latest-snapshot-per-content pattern as every other metrics query above.
    @Query(value = """
            SELECT c.campaign_id AS campaignId,
                   COALESCE(SUM(latest.view_count), 0) AS totalViews
            FROM content c
            LEFT JOIN (
                SELECT cms.content_id, cms.view_count,
                       ROW_NUMBER() OVER (PARTITION BY cms.content_id ORDER BY cms.fetched_at DESC) AS rn
                FROM content_metrics_snapshots cms
            ) latest ON latest.content_id = c.id AND latest.rn = 1
            WHERE c.status = 'PUBLISHED' AND c.campaign_id IN (:campaignIds)
            GROUP BY c.campaign_id
            """,
            nativeQuery = true)
    List<CampaignViewsRow> sumViewsByCampaignIds(@Param("campaignIds") Collection<Long> campaignIds);
}
