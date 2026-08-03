package club.escobar.entity.enums;

public enum CampaignStatus {
    // Manually selectable, persisted as-is.
    DRAFT,
    CANCELLED,

    // Persisted sentinel meaning "not draft, not cancelled - let the dates decide the phase".
    // Never returned to API clients; CampaignMapper always resolves it to one of the computed
    // values below via Campaign#getEffectiveStatus().
    PUBLISHED,

    // Computed-only, never persisted: derived live from today vs. the campaign's submission/publish
    // dates so the phase can never go stale the way a manually-set status could.
    UPCOMING,
    LIVE,
    COMPLETED
}
