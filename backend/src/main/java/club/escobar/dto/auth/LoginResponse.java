package club.escobar.dto.auth;

/** Either {@code auth} is populated (login complete), or {@code challengeToken} is (2FA code required next). */
public record LoginResponse(
        boolean requiresTwoFactor,
        String challengeToken,
        AuthResponse auth
) {
    public static LoginResponse success(AuthResponse auth) {
        return new LoginResponse(false, null, auth);
    }

    public static LoginResponse twoFactorRequired(String challengeToken) {
        return new LoginResponse(true, challengeToken, null);
    }
}
