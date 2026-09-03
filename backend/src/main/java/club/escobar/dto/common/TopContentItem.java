package club.escobar.dto.common;

public record TopContentItem(
        Long contentId,
        String creatorDisplayName,
        String campaignTitle,
        String mediaType,
        long views,
        long likes,
        long comments,
        double engagementRate,
        String postUrl
) {
}
