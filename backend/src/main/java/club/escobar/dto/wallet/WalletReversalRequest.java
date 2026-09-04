package club.escobar.dto.wallet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WalletReversalRequest(
        @NotBlank @Size(max = 500) String note
) {
}
