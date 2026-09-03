package club.escobar.repository;

public interface TopContentRow {
    Long getContentId();

    String getCreatorDisplayName();

    String getCampaignTitle();

    String getMediaType();

    Long getViews();

    Long getLikes();

    Long getComments();

    String getPostUrl();
}
