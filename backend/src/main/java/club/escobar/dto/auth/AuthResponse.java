package club.escobar.dto.auth;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Long sessionId,
        UserSummaryResponse user
) {
}
