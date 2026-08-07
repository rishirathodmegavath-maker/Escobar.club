package club.escobar.dto.admin;

import club.escobar.entity.enums.ContentStatus;

import java.time.Instant;

public record AdminContentSummaryResponse(
        Long id,
        String creatorDisplayName,
        String creatorEmail,
        String businessCompanyName,
        String campaignTitle,
        ContentStatus status,
        String postUrl,
        Instant publishedAt,
        Long likeCount,
        Long commentCount,
        Long viewCount,
        Instant metricsLastSyncedAt
) {
}
