package club.escobar.dto.drafts;

import java.time.Instant;

public record FormDraftResponse(
        String draftKey,
        String payload,
        Instant updatedAt
) {
}
