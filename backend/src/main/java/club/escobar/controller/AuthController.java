package club.escobar.controller;

import club.escobar.dto.auth.AuthResponse;
import club.escobar.dto.auth.ForgotPasswordRequest;
import club.escobar.dto.auth.GoogleAuthRequest;
import club.escobar.dto.auth.LoginRequest;
import club.escobar.dto.auth.LoginResponse;
import club.escobar.dto.auth.OtpVerifyRequest;
import club.escobar.dto.auth.RefreshRequest;
import club.escobar.dto.auth.RegisterRequest;
import club.escobar.dto.auth.RegisterResponse;
import club.escobar.dto.auth.ResetPasswordRequest;
import club.escobar.dto.auth.TwoFactorVerifyRequest;
import club.escobar.dto.auth.VerifyEmailRequest;
import club.escobar.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, clientIp(httpRequest), userAgent(httpRequest)));
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<AuthResponse> verifyTwoFactor(@Valid @RequestBody TwoFactorVerifyRequest request,
                                                          HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.verifyTwoFactorLogin(request, clientIp(httpRequest), userAgent(httpRequest)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken(), clientIp(httpRequest), userAgent(httpRequest)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> google(@Valid @RequestBody GoogleAuthRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.googleAuth(request, clientIp(httpRequest), userAgent(httpRequest)));
    }

    @PostMapping("/otp/request")
    public ResponseEntity<Void> requestOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestLoginOtp(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<LoginResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.verifyLoginOtp(request, clientIp(httpRequest), userAgent(httpRequest)));
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
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request));
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
}
