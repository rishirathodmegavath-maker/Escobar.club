package club.escobar.dto.metrics;

public record LeaderboardEntryResponse(
        int rank,
        Long creatorId,
        String creatorDisplayName,
        String creatorProfilePictureUrl,
        Long totalViews,
        Long publishedContentCount
) {
}
