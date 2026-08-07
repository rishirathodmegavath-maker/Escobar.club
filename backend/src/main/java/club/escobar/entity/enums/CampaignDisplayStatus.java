package club.escobar.entity.enums;

// Admin-curated label shown to creators, independent of Campaign#getEffectiveStatus() (which is
// derived automatically from dates). Null on the entity means the admin hasn't set one yet.
public enum CampaignDisplayStatus {
    UPCOMING,
    LIVE,
    HOT,
    CLOSED
}
