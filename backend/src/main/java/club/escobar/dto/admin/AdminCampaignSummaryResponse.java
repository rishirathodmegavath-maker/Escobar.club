package club.escobar.dto.admin;

import club.escobar.entity.enums.ApprovalStatus;
import club.escobar.entity.enums.CampaignDisplayStatus;
import club.escobar.entity.enums.CampaignStatus;

import java.time.Instant;
import java.time.LocalDate;

public record AdminCampaignSummaryResponse(
        Long id,
        String title,
        String businessCompanyName,
        CampaignStatus status,
        ApprovalStatus approvalStatus,
        CampaignDisplayStatus adminDisplayStatus,
        LocalDate publishStartAt,
        LocalDate publishEndAt,
        Instant createdAt
) {
}
