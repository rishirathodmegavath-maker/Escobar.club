package club.escobar.dto.creator;

public record ContentStatusCounts(
        long submitted,
        long changesRequested,
        long approved,
        long pendingLinkReview,
        long published,
        long rejected
) {
}
