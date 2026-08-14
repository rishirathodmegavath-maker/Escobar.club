package club.escobar.dto.campaign;

import java.time.Instant;
import java.time.LocalDate;

public record CampaignScheduleChangeResponse(
        Long id,
        LocalDate oldSubmissionOpenAt,
        LocalDate oldSubmissionDeadline,
        LocalDate oldPublishStartAt,
        LocalDate oldPublishEndAt,
        LocalDate newSubmissionOpenAt,
        LocalDate newSubmissionDeadline,
        LocalDate newPublishStartAt,
        LocalDate newPublishEndAt,
        Long changedByUserId,
        Instant changedAt
) {
}
