package club.escobar.dto.auth;

import club.escobar.entity.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleAuthRequest(
        @NotBlank String idToken,
        UserRole role,
        @Size(max = 150) String displayName,
        @Size(max = 20) String gstNumber,
        @Size(max = 150) String contactPersonName,
        @Size(max = 20) String mobileNumber
) {
}
