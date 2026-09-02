package club.escobar.dto.content;

public record ContentBulkReviewFailure(
        Long contentId,
        String reason
) {
}
