import { apiClient } from "./client";
import type { Campaign, CampaignAnalytics, CampaignCategory, CampaignScheduleChange, ManualCampaignStatus, PageResponse } from "@/types";

export interface CampaignSearchParams {
  search?: string;
  category?: CampaignCategory;
  page?: number;
  size?: number;
}

export interface CampaignFormPayload {
  title: string;
  description: string;
  submissionOpenAt: string;
  submissionDeadline: string;
  publishStartAt: string;
  publishEndAt: string;
  ratePerThousandViewsInr: number;
  urgent: boolean;
  maxBudgetInr: number | null;
}

export interface CampaignUpdatePayload extends CampaignFormPayload {
  status: ManualCampaignStatus;
}

export interface CampaignSchedulePayload {
  submissionOpenAt: string;
  submissionDeadline: string;
  publishStartAt: string;
  publishEndAt: string;
}

export const campaignsApi = {
  searchPublic: (params: CampaignSearchParams) =>
    apiClient
      .get<PageResponse<Campaign>>("/campaigns", { params: { size: 12, ...params } })
      .then((r) => r.data),
  getById: (id: number) => apiClient.get<Campaign>(`/campaigns/${id}`).then((r) => r.data),
  mine: (page = 0, size = 10) =>
    apiClient.get<PageResponse<Campaign>>("/campaigns/mine", { params: { page, size } }).then((r) => r.data),
  create: (payload: CampaignFormPayload) =>
    apiClient.post<Campaign>("/campaigns", payload).then((r) => r.data),
  update: (id: number, payload: CampaignUpdatePayload) =>
    apiClient.put<Campaign>(`/campaigns/${id}`, payload).then((r) => r.data),
  remove: (id: number) => apiClient.delete<void>(`/campaigns/${id}`).then((r) => r.data),
  updateSchedule: (id: number, payload: CampaignSchedulePayload) =>
    apiClient.patch<Campaign>(`/campaigns/${id}/schedule`, payload).then((r) => r.data),
  scheduleHistory: (id: number) =>
    apiClient.get<CampaignScheduleChange[]>(`/campaigns/${id}/schedule-history`).then((r) => r.data),
  analytics: (id: number) => apiClient.get<CampaignAnalytics>(`/campaigns/${id}/analytics`).then((r) => r.data),
};
