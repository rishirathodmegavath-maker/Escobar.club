package club.escobar.service.impl;

import club.escobar.config.MetricsSyncProperties;
import club.escobar.dto.common.PageResponse;
import club.escobar.dto.metrics.ContentMetricsSnapshotResponse;
import club.escobar.dto.metrics.LeaderboardEntryResponse;
import club.escobar.entity.Content;
import club.escobar.entity.ContentMetricsSnapshot;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.exception.ForbiddenActionException;
import club.escobar.exception.InvalidStateTransitionException;
import club.escobar.exception.RateLimitExceededException;
import club.escobar.exception.ResourceNotFoundException;
import club.escobar.integration.apify.ApifyInstagramClient;
import club.escobar.integration.apify.ApifyPostMetrics;
import club.escobar.mapper.ContentMetricsMapper;
import club.escobar.repository.ContentMetricsSnapshotRepository;
import club.escobar.repository.ContentRepository;
import club.escobar.repository.LeaderboardRow;
import club.escobar.service.ContentMetricsService;
import club.escobar.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentMetricsServiceImpl implements ContentMetricsService {

    private static final Logger log = LoggerFactory.getLogger(ContentMetricsServiceImpl.class);

    private final ContentRepository contentRepository;
    private final ContentMetricsSnapshotRepository contentMetricsSnapshotRepository;
    private final ApifyInstagramClient apifyInstagramClient;
    private final MetricsSyncProperties metricsSyncProperties;
    private final ContentMetricsMapper contentMetricsMapper;
    private final PayoutService payoutService;

    @Override
    @Transactional
    public ContentMetricsSnapshotResponse syncMetrics(Long requestingUserId, Long contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id " + contentId));

        boolean isCreatorOwner = content.getCreator().getId().equals(requestingUserId);
        boolean isBusinessOwner = content.getBusiness().getId().equals(requestingUserId);
        if (!isCreatorOwner && !isBusinessOwner) {
            throw new ForbiddenActionException("You do not have access to this content's metrics");
        }
        if (content.getStatus() != ContentStatus.PUBLISHED) {
            throw new InvalidStateTransitionException(
                    "Metrics can only be synced for published content (current status: " + content.getStatus() + ")");
        }

        contentMetricsSnapshotRepository.findTopByContent_IdOrderByFetchedAtDesc(contentId)
                .ifPresent(last -> {
                    Instant nextEligible = last.getFetchedAt().plus(metricsSyncProperties.minIntervalMinutes(), ChronoUnit.MINUTES);
                    if (Instant.now().isBefore(nextEligible)) {
                        throw new RateLimitExceededException(
                                "Metrics were synced recently; try again after " + DateTimeFormatter.ISO_INSTANT.format(nextEligible));
                    }
                });

        ApifyPostMetrics metrics = apifyInstagramClient.fetchPostMetrics(content.getPostUrl());

        ContentMetricsSnapshot snapshot = ContentMetricsSnapshot.builder()
                .likeCount(metrics.likeCount())
                .commentCount(metrics.commentCount())
                .viewCount(metrics.viewCount())
                .rawPayload(metrics.rawJson())
                .build();
        content.addMetricsSnapshot(snapshot);
        contentRepository.save(content);
        payoutService.recalculate(contentId);

        log.info("User id={} synced metrics for content id={}: likes={} comments={} views={}",
                requestingUserId, contentId, metrics.likeCount(), metrics.commentCount(), metrics.viewCount());
        return contentMetricsMapper.toResponse(snapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContentMetricsSnapshotResponse> getMetricsHistory(Long requestingUserId, Long contentId, Pageable pageable) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id " + contentId));

        boolean isCreatorOwner = content.getCreator().getId().equals(requestingUserId);
        boolean isBusinessOwner = content.getBusiness().getId().equals(requestingUserId);
        if (!isCreatorOwner && !isBusinessOwner) {
            throw new ForbiddenActionException("You do not have access to this content's metrics");
        }

        Page<ContentMetricsSnapshotResponse> page = contentMetricsSnapshotRepository
                .findByContent_IdOrderByFetchedAtDesc(contentId, pageable)
                .map(contentMetricsMapper::toResponse);
        return PageResponse.of(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeaderboardEntryResponse> businessLeaderboard(Long requestingBusinessUserId, Long businessId, Pageable pageable) {
        if (!requestingBusinessUserId.equals(businessId)) {
            throw new ForbiddenActionException("You may only view your own leaderboard");
        }
        return toLeaderboardResponse(contentRepository.findBusinessLeaderboard(businessId, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeaderboardEntryResponse> globalLeaderboard(Pageable pageable) {
        return toLeaderboardResponse(contentRepository.findGlobalLeaderboard(pageable));
    }

    private PageResponse<LeaderboardEntryResponse> toLeaderboardResponse(Page<LeaderboardRow> rows) {
        List<LeaderboardRow> content = rows.getContent();
        List<LeaderboardEntryResponse> entries = new ArrayList<>(content.size());
        for (int i = 0; i < content.size(); i++) {
            LeaderboardRow row = content.get(i);
            int rank = (int) rows.getPageable().getOffset() + i + 1;
            entries.add(new LeaderboardEntryResponse(
                    rank, row.getCreatorId(), row.getCreatorDisplayName(), row.getCreatorProfilePictureUrl(),
                    row.getTotalViews(), row.getPublishedContentCount()));
        }
        return new PageResponse<>(entries, rows.getNumber(), rows.getSize(), rows.getTotalElements(), rows.getTotalPages(), rows.isLast());
    }
}
