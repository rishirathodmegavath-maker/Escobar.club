import { apiClient } from "./client";
import type { SessionSummary, TwoFactorSetupResponse, UserSummary } from "@/types";

export interface SetPasswordPayload {
  currentPassword?: string;
  newPassword: string;
}

export const accountApi = {
  setPassword: (payload: SetPasswordPayload) =>
    apiClient.post<UserSummary>("/account/password", payload).then((r) => r.data),
  setupTwoFactor: () => apiClient.post<TwoFactorSetupResponse>("/account/2fa/setup").then((r) => r.data),
  confirmTwoFactor: (code: string) =>
    apiClient.post<UserSummary>("/account/2fa/confirm", { code }).then((r) => r.data),
  disableTwoFactor: (code: string) =>
    apiClient.post<UserSummary>("/account/2fa/disable", { code }).then((r) => r.data),
  listSessions: () => apiClient.get<SessionSummary[]>("/account/sessions").then((r) => r.data),
  revokeSession: (id: number) => apiClient.delete<void>(`/account/sessions/${id}`).then((r) => r.data),
};
