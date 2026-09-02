package club.escobar.dto.content;

import java.util.List;

public record ContentBulkReviewResponse(
        int succeeded,
        List<ContentBulkReviewFailure> failures
) {
}
