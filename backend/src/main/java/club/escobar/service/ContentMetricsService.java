package club.escobar.service;

import club.escobar.dto.common.PageResponse;
import club.escobar.dto.metrics.ContentMetricsSnapshotResponse;
import club.escobar.dto.metrics.LeaderboardEntryResponse;
import org.springframework.data.domain.Pageable;

public interface ContentMetricsService {

    ContentMetricsSnapshotResponse syncMetrics(Long requestingUserId, Long contentId);

    // Called by the scheduled refresh job only - walks a batch of published content whose metrics
    // are stale/missing and refreshes them, bypassing the per-user ownership check that syncMetrics
    // enforces (there is no requesting user here). One item's failure is logged and skipped rather
    // than aborting the rest of the batch.
    void syncDueMetrics(int batchSize);

    PageResponse<ContentMetricsSnapshotResponse> getMetricsHistory(Long requestingUserId, Long contentId, Pageable pageable);

    PageResponse<LeaderboardEntryResponse> businessLeaderboard(Long requestingBusinessUserId, Long businessId, Pageable pageable);

    PageResponse<LeaderboardEntryResponse> globalLeaderboard(Pageable pageable);
}
