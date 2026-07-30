package club.escobar.dto.auth;

import club.escobar.entity.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotNull UserRole role,
        @NotBlank @Size(max = 150) String displayName,
        @Size(max = 20) String gstNumber,
        @Size(max = 150) String contactPersonName,
        @Size(max = 20) String mobileNumber
) {
}
