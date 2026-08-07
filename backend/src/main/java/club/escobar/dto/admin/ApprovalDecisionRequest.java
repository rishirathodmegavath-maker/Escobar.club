package club.escobar.dto.admin;

import club.escobar.entity.enums.ApprovalStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApprovalDecisionRequest(
        @NotNull ApprovalStatus status,
        @Size(max = 2000) String note
) {
}
