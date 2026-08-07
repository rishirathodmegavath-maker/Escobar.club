package club.escobar.dto.admin;

import club.escobar.entity.enums.CampaignDisplayStatus;
import jakarta.validation.constraints.NotNull;

public record CampaignDisplayStatusUpdateRequest(
        @NotNull CampaignDisplayStatus status
) {
}
