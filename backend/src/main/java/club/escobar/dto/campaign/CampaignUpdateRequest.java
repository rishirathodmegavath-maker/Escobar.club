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
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @DecimalMin("0.01") BigDecimal ratePerThousandViewsInr,
        @NotNull CampaignStatus status,
        boolean urgent
) {
}
