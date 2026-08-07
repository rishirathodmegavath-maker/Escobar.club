package club.escobar.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TwoFactorDisableRequest(
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits") String code
) {
}
