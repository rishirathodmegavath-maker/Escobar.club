package club.escobar.service;

import club.escobar.dto.auth.AuthResponse;
import club.escobar.dto.auth.ForgotPasswordRequest;
import club.escobar.dto.auth.GoogleAuthRequest;
import club.escobar.dto.auth.LoginRequest;
import club.escobar.dto.auth.LoginResponse;
import club.escobar.dto.auth.OtpVerifyRequest;
import club.escobar.dto.auth.RegisterRequest;
import club.escobar.dto.auth.RegisterResponse;
import club.escobar.dto.auth.ResetPasswordRequest;
import club.escobar.dto.auth.SetPasswordRequest;
import club.escobar.dto.auth.TwoFactorVerifyRequest;
import club.escobar.dto.auth.UserSummaryResponse;
import club.escobar.dto.auth.VerifyEmailRequest;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request, String ipAddress, String userAgent);

    AuthResponse verifyTwoFactorLogin(TwoFactorVerifyRequest request, String ipAddress, String userAgent);

    AuthResponse refresh(String refreshToken, String ipAddress, String userAgent);

    void logout(String refreshToken);

    LoginResponse googleAuth(GoogleAuthRequest request, String ipAddress, String userAgent);

    void requestLoginOtp(ForgotPasswordRequest request);

    LoginResponse verifyLoginOtp(OtpVerifyRequest request, String ipAddress, String userAgent);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    UserSummaryResponse setPassword(Long userId, SetPasswordRequest request);

    AuthResponse verifyEmail(VerifyEmailRequest request);

    void resendVerification(ForgotPasswordRequest request);
}
