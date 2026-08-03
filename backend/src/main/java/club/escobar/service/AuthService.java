package club.escobar.service;

import club.escobar.dto.auth.AuthResponse;
import club.escobar.dto.auth.ForgotPasswordRequest;
import club.escobar.dto.auth.GoogleAuthRequest;
import club.escobar.dto.auth.LoginRequest;
import club.escobar.dto.auth.RegisterRequest;
import club.escobar.dto.auth.RegisterResponse;
import club.escobar.dto.auth.ResetPasswordRequest;
import club.escobar.dto.auth.SetPasswordRequest;
import club.escobar.dto.auth.UserSummaryResponse;
import club.escobar.dto.auth.VerifyEmailRequest;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String refreshToken);

    void logout(String refreshToken);

    AuthResponse googleAuth(GoogleAuthRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    UserSummaryResponse setPassword(Long userId, SetPasswordRequest request);

    AuthResponse verifyEmail(VerifyEmailRequest request);

    void resendVerification(ForgotPasswordRequest request);
}
