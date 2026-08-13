import { apiClient } from "./client";
import type { AuthResponse, LoginResponse, UserRole } from "@/types";

export interface RegisterPayload {
  email: string;
  password: string;
  role: UserRole;
  displayName: string;
  gstNumber?: string;
  contactPersonName?: string;
  mobileNumber?: string;
}

export interface RegisterResult {
  message: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface GoogleAuthPayload {
  idToken: string;
  role?: UserRole;
  displayName?: string;
  gstNumber?: string;
  contactPersonName?: string;
  mobileNumber?: string;
}

export interface ForgotPasswordPayload {
  email: string;
}

export interface ResetPasswordPayload {
  token: string;
  newPassword: string;
}

export interface VerifyEmailPayload {
  token: string;
}

export interface ResendVerificationPayload {
  email: string;
}

export interface TwoFactorVerifyPayload {
  challengeToken: string;
  code: string;
}

export interface OtpRequestPayload {
  email: string;
}

export interface OtpVerifyPayload {
  email: string;
  code: string;
}

export const authApi = {
  register: (payload: RegisterPayload) =>
    apiClient.post<RegisterResult>("/auth/register", payload).then((r) => r.data),
  login: (payload: LoginPayload) =>
    apiClient.post<LoginResponse>("/auth/login", payload).then((r) => r.data),
  verifyTwoFactor: (payload: TwoFactorVerifyPayload) =>
    apiClient.post<AuthResponse>("/auth/2fa/verify", payload).then((r) => r.data),
  requestOtp: (payload: OtpRequestPayload) =>
    apiClient.post<void>("/auth/otp/request", payload).then((r) => r.data),
  verifyOtp: (payload: OtpVerifyPayload) =>
    apiClient.post<LoginResponse>("/auth/otp/verify", payload).then((r) => r.data),
  logout: () => apiClient.post("/auth/logout"),
  googleAuth: (payload: GoogleAuthPayload) =>
    apiClient.post<LoginResponse>("/auth/google", payload).then((r) => r.data),
  forgotPassword: (payload: ForgotPasswordPayload) =>
    apiClient.post<void>("/auth/forgot-password", payload).then((r) => r.data),
  resetPassword: (payload: ResetPasswordPayload) =>
    apiClient.post<void>("/auth/reset-password", payload).then((r) => r.data),
  verifyEmail: (payload: VerifyEmailPayload) =>
    apiClient.post<AuthResponse>("/auth/verify-email", payload).then((r) => r.data),
  resendVerification: (payload: ResendVerificationPayload) =>
    apiClient.post<void>("/auth/resend-verification", payload).then((r) => r.data),
};
