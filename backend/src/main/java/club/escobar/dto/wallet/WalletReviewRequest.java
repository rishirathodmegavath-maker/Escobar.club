package club.escobar.dto.wallet;

import club.escobar.entity.enums.WalletTransactionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// decision must be CONFIRMED or REJECTED - validated in WalletServiceImpl.reviewTopUp, same
// spot AdminServiceImpl.reviewContentLink validates its own request.status() the same way.
public record WalletReviewRequest(
        @NotNull WalletTransactionStatus decision,
        @Size(max = 500) String note
) {
}
