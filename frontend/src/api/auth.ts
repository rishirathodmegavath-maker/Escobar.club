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

export interface LoginPayload {
  email: string;
  password: string;
}

export const authApi = {
  register: (payload: RegisterPayload) =>
    apiClient.post<AuthResponse>("/auth/register", payload).then((r) => r.data),
  login: (payload: LoginPayload) =>
    apiClient.post<AuthResponse>("/auth/login", payload).then((r) => r.data),
  logout: (refreshToken: string) => apiClient.post("/auth/logout", { refreshToken }),
};
