package club.escobar.dto.kyc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatorKycSubmitRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Must be a valid 10-character PAN number (e.g. ABCDE1234F)")
        String panNumber,

        @NotBlank @Size(max = 150)
        String nameOnPan,

        // Optional: omitted on a resubmit that keeps the previously-uploaded document. Required
        // (enforced in CreatorKycServiceImpl.submit) when there is no existing document to fall back to.
        @Size(max = 500)
        String documentKey
) {
}
