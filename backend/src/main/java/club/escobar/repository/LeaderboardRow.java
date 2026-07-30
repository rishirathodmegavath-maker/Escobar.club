package club.escobar.repository;

public interface LeaderboardRow {
    Long getCreatorId();

    String getCreatorDisplayName();

    String getCreatorProfilePictureUrl();

    Long getTotalViews();

    Long getPublishedContentCount();
}
