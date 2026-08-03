import { apiClient } from "./client";
import type { AuthResponse, UserRole } from "@/types";

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

export const authApi = {
  register: (payload: RegisterPayload) =>
    apiClient.post<RegisterResult>("/auth/register", payload).then((r) => r.data),
  login: (payload: LoginPayload) =>
    apiClient.post<AuthResponse>("/auth/login", payload).then((r) => r.data),
  logout: (refreshToken: string) => apiClient.post("/auth/logout", { refreshToken }),
  googleAuth: (payload: GoogleAuthPayload) =>
    apiClient.post<AuthResponse>("/auth/google", payload).then((r) => r.data),
  forgotPassword: (payload: ForgotPasswordPayload) =>
    apiClient.post<void>("/auth/forgot-password", payload).then((r) => r.data),
  resetPassword: (payload: ResetPasswordPayload) =>
    apiClient.post<void>("/auth/reset-password", payload).then((r) => r.data),
  verifyEmail: (payload: VerifyEmailPayload) =>
    apiClient.post<AuthResponse>("/auth/verify-email", payload).then((r) => r.data),
  resendVerification: (payload: ResendVerificationPayload) =>
    apiClient.post<void>("/auth/resend-verification", payload).then((r) => r.data),
};
