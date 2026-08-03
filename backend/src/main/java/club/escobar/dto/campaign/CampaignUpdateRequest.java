package club.escobar.dto.campaign;

import club.escobar.entity.enums.CampaignStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CampaignUpdateRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 4000) String description,
        @NotNull LocalDate submissionOpenAt,
        @NotNull LocalDate submissionDeadline,
        @NotNull LocalDate publishStartAt,
        @NotNull LocalDate publishEndAt,
        @NotNull @DecimalMin("0.01") BigDecimal ratePerThousandViewsInr,
        // Only DRAFT, PUBLISHED (i.e. "auto - let dates decide") and CANCELLED are valid here.
        // UPCOMING/LIVE/COMPLETED are computed and rejected if sent - see CampaignServiceImpl.
        @NotNull CampaignStatus status,
        boolean urgent
) {
}
