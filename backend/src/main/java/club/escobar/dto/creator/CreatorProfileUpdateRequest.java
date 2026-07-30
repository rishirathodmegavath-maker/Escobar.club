package club.escobar.dto.creator;

import jakarta.validation.constraints.*;

import java.util.List;

public record CreatorProfileUpdateRequest(
        @NotBlank @Size(max = 120) String displayName,
        @Size(max = 4000) String bio,
        @Size(max = 500) String profilePictureUrl,
        @Size(max = 80) String niche,
        boolean openToOtherNiches,
        @NotBlank @Pattern(regexp = "^https?://(www\\.)?instagram\\.com/.+", message = "Enter a valid Instagram profile URL")
        @Size(max = 500) String instagramProfileUrl,
        @NotNull @PositiveOrZero Long followerCount,
        @Size(max = 30) List<@NotBlank @Size(max = 500) String> portfolioLinks
) {
}
