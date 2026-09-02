package club.escobar.dto.campaign;

import club.escobar.entity.enums.ApprovalStatus;
import club.escobar.entity.enums.CampaignDisplayStatus;
import club.escobar.entity.enums.CampaignStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CampaignResponse(
        Long id,
        Long businessId,
        String businessCompanyName,
        String businessLogoUrl,
        String title,
        String description,
        LocalDate submissionOpenAt,
        LocalDate submissionDeadline,
        LocalDate publishStartAt,
        LocalDate publishEndAt,
        BigDecimal ratePerThousandViewsInr,
        CampaignStatus status,
        boolean acceptingSubmissions,
        boolean urgent,
        boolean hot,
        // Null means no cap set (unlimited). committedBudgetInr sums PENDING_KYC+PAYABLE+PAID
        // payouts for this campaign - computed server-side, same pattern as `hot` below.
        BigDecimal maxBudgetInr,
        BigDecimal committedBudgetInr,
        ApprovalStatus approvalStatus,
        CampaignDisplayStatus adminDisplayStatus,
        // Computed server-side from effective status only (DRAFT/UPCOMING = true) - the frontend
        // must gate the "Change Schedule" action on this, not re-derive the rule from `status` itself.
        boolean canChangeSchedule,
        Instant createdAt,
        Instant updatedAt,
        // Human-readable reason submissions are closed (date-window reasons, or the budget cap) -
        // null while accepting. Unlike maxBudgetInr/committedBudgetInr this never reveals a
        // business's actual budget figures, so it's safe to expose on every endpoint including the
        // public/creator-facing ones.
        String submissionClosedReason
) {
}
