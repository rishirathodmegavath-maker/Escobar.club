package club.escobar.entity.enums;

public enum ContentStatus {
    DRAFT,
    SUBMITTED,
    CHANGES_REQUESTED,
    APPROVED,
    REJECTED,
    // Creator has submitted their live post link; awaiting admin approval before it's PUBLISHED.
    PENDING_LINK_REVIEW,
    PUBLISHED
}
