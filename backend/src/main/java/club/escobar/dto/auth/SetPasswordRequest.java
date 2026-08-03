package club.escobar.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetPasswordRequest(
        String currentPassword,
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {
}
