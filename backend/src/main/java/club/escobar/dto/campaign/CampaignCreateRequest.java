package club.escobar.dto.campaign;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CampaignCreateRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 4000) String description,
        @NotNull LocalDate submissionOpenAt,
        @NotNull LocalDate submissionDeadline,
        @NotNull LocalDate publishStartAt,
        @NotNull LocalDate publishEndAt,
        @NotNull @DecimalMin(value = "100", message = "Rate must be at least ₹100 per 1,000 views") BigDecimal ratePerThousandViewsInr,
        boolean urgent
) {
}
