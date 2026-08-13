package club.escobar.controller;

import club.escobar.config.JwtProperties;
import club.escobar.dto.auth.AuthResponse;
import club.escobar.dto.auth.ForgotPasswordRequest;
import club.escobar.dto.auth.GoogleAuthRequest;
import club.escobar.dto.auth.LoginRequest;
import club.escobar.dto.auth.LoginResponse;
import club.escobar.dto.auth.OtpVerifyRequest;
import club.escobar.dto.auth.RegisterRequest;
import club.escobar.dto.auth.RegisterResponse;
import club.escobar.dto.auth.ResetPasswordRequest;
import club.escobar.dto.auth.TwoFactorVerifyRequest;
import club.escobar.dto.auth.VerifyEmailRequest;
import club.escobar.exception.ApiException;
import club.escobar.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirements
public class AuthController {

    // Scoped to /api/auth (not the whole site) so it isn't sent on every unrelated API call;
    // SameSite=Strict means it's never sent cross-site at all, which is sufficient CSRF
    // protection for this cookie on its own - no separate CSRF token needed.
    private static final String REFRESH_COOKIE = "escobar_rt";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, clientIp(httpRequest), userAgent(httpRequest));
        return withRefreshCookie(response, httpRequest.isSecure());
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<AuthResponse> verifyTwoFactor(@Valid @RequestBody TwoFactorVerifyRequest request,
                                                          HttpServletRequest httpRequest) {
        AuthResponse auth = authService.verifyTwoFactorLogin(request, clientIp(httpRequest), userAgent(httpRequest));
        return withRefreshCookie(auth, httpRequest.isSecure());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
                                                 HttpServletRequest httpRequest) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or has been revoked");
        }
        AuthResponse auth = authService.refresh(refreshToken, clientIp(httpRequest), userAgent(httpRequest));
        return withRefreshCookie(auth, httpRequest.isSecure());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
                                        HttpServletRequest httpRequest) {
        if (StringUtils.hasText(refreshToken)) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie(httpRequest.isSecure()).toString())
                .build();
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> google(@Valid @RequestBody GoogleAuthRequest request, HttpServletRequest httpRequest) {
        LoginResponse response = authService.googleAuth(request, clientIp(httpRequest), userAgent(httpRequest));
        return withRefreshCookie(response, httpRequest.isSecure());
    }

    @PostMapping("/otp/request")
    public ResponseEntity<Void> requestOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestLoginOtp(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<LoginResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request, HttpServletRequest httpRequest) {
        LoginResponse response = authService.verifyLoginOtp(request, clientIp(httpRequest), userAgent(httpRequest));
        return withRefreshCookie(response, httpRequest.isSecure());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request, HttpServletRequest httpRequest) {
        AuthResponse auth = authService.verifyEmail(request);
        return withRefreshCookie(auth, httpRequest.isSecure());
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.resendVerification(request);
        return ResponseEntity.noContent().build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    /** Moves the refresh token from the response body into an httpOnly cookie; the JSON body
     * keeps everything else (access token, session id, user) but omits refreshToken (Jackson
     * drops it - see app.jackson.default-property-inclusion=non_null). {@code secure} is driven
     * by the actual request scheme (via Caddy's X-Forwarded-Proto + forward-headers-strategy)
     * rather than hardcoded, so the cookie still round-trips over plain HTTP in local dev. */
    private ResponseEntity<AuthResponse> withRefreshCookie(AuthResponse auth, boolean secure) {
        AuthResponse body = new AuthResponse(auth.accessToken(), null, auth.sessionId(), auth.user());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie(auth.refreshToken(), secure).toString()).body(body);
    }

    private ResponseEntity<LoginResponse> withRefreshCookie(LoginResponse response, boolean secure) {
        if (response.requiresTwoFactor()) {
            return ResponseEntity.ok(response);
        }
        AuthResponse sanitizedAuth = new AuthResponse(response.auth().accessToken(), null, response.auth().sessionId(), response.auth().user());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(response.auth().refreshToken(), secure).toString())
                .body(LoginResponse.success(sanitizedAuth));
    }

    private ResponseCookie refreshCookie(String token, boolean secure) {
        return ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(Duration.ofDays(jwtProperties.refreshTokenTtlDays()))
                .build();
    }

    private ResponseCookie clearRefreshCookie(boolean secure) {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(0)
                .build();
    }
}
