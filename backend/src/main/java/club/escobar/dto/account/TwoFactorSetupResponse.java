package club.escobar.dto.account;

public record TwoFactorSetupResponse(
        String secret,
        String otpauthUri
) {
}
