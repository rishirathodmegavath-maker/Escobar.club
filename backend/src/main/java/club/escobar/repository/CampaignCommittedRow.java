package club.escobar.repository;

import java.math.BigDecimal;

public interface CampaignCommittedRow {
    Long getCampaignId();

    BigDecimal getCommitted();
}
