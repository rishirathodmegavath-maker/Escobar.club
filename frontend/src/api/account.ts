import { apiClient } from "./client";
import type { UserSummary } from "@/types";

export interface SetPasswordPayload {
  currentPassword?: string;
  newPassword: string;
}

export const accountApi = {
  setPassword: (payload: SetPasswordPayload) =>
    apiClient.post<UserSummary>("/account/password", payload).then((r) => r.data),
};
