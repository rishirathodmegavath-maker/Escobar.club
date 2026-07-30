package club.escobar.dto.creator;

import java.time.Instant;
import java.util.List;

public record CreatorProfileResponse(
        Long id,
        Long userId,
        String email,
        String displayName,
        String bio,
        String profilePictureUrl,
        String niche,
        boolean openToOtherNiches,
        String instagramProfileUrl,
        Long followerCount,
        List<String> portfolioLinks,
        Instant createdAt
) {
}
