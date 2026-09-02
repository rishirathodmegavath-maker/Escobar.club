package club.escobar.repository;

import club.escobar.entity.Content;
import club.escobar.entity.enums.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentRepository extends JpaRepository<Content, Long> {

    Page<Content> findByBusiness_Id(Long businessId, Pageable pageable);

    Page<Content> findByBusiness_IdAndStatus(Long businessId, ContentStatus status, Pageable pageable);

    long countByBusiness_IdAndStatus(Long businessId, ContentStatus status);

    Page<Content> findByCreator_Id(Long creatorId, Pageable pageable);

    Page<Content> findByStatus(ContentStatus status, Pageable pageable);

    boolean existsByCreator_IdAndBusiness_Id(Long creatorId, Long businessId);

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
}
