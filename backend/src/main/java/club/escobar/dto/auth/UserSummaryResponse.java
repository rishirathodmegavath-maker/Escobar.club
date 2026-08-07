package club.escobar.dto.auth;

import club.escobar.entity.enums.UserRole;

public record UserSummaryResponse(
        Long id,
        String email,
        UserRole role,
        boolean hasPassword,
        boolean twoFactorEnabled
) {
}
