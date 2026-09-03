package club.escobar.repository;

import java.math.BigDecimal;

public interface CampaignAggregateRow {
    Long getCampaignId();

    Long getViews();

    BigDecimal getEarnings();
}
