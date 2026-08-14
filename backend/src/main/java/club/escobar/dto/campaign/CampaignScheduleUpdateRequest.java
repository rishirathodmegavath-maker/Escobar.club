package club.escobar.dto.campaign;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CampaignScheduleUpdateRequest(
        @NotNull LocalDate submissionOpenAt,
        @NotNull LocalDate submissionDeadline,
        @NotNull LocalDate publishStartAt,
        @NotNull LocalDate publishEndAt
) {
}
