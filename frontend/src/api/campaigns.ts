import { apiClient } from "./client";
import type { Campaign, CampaignCategory, CampaignStatus, PageResponse } from "@/types";

export interface CampaignSearchParams {
  search?: string;
  category?: CampaignCategory;
  page?: number;
  size?: number;
}

export interface CampaignFormPayload {
  title: string;
  description: string;
  startDate: string;
  endDate: string;
  ratePerThousandViewsInr: number;
  urgent: boolean;
}

export interface CampaignUpdatePayload extends CampaignFormPayload {
  status: CampaignStatus;
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
};
