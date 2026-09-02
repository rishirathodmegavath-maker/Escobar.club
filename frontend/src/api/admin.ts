import { apiClient } from "./client";
import type {
  AdminBusinessSummary,
  AdminCampaignSummary,
  AdminContentSummary,
  AdminCreatorSummary,
  AdminDashboardSummary,
  ApprovalStatus,
  CampaignDisplayStatus,
  ContentStatus,
  CreatorKycReviewDetail,
  KycStatus,
  PageResponse,
} from "@/types";

export interface AdminListParams {
  status?: ApprovalStatus;
  search?: string;
  page?: number;
  size?: number;
}

export interface AdminContentListParams {
  status?: ContentStatus;
  page?: number;
  size?: number;
}

export interface KycReviewPayload {
  status: KycStatus;
  reviewNote?: string;
}

export interface ApprovalDecisionPayload {
  status: ApprovalStatus;
  note?: string;
}

export const adminApi = {
  dashboard: () => apiClient.get<AdminDashboardSummary>("/admin/dashboard").then((r) => r.data),

  listCreators: (params: { status?: KycStatus; page?: number; size?: number } = {}) =>
    apiClient
      .get<PageResponse<AdminCreatorSummary>>("/admin/creators", { params: { size: 20, ...params } })
      .then((r) => r.data),
  reviewCreatorKyc: (userId: number, payload: KycReviewPayload) =>
    apiClient.patch<CreatorKycReviewDetail>(`/admin/creators/${userId}/kyc`, payload).then((r) => r.data),
  setCreatorActive: (userId: number, active: boolean) =>
    apiClient.patch<AdminCreatorSummary>(`/admin/creators/${userId}/status`, { active }).then((r) => r.data),

  listBusinesses: (params: AdminListParams) =>
    apiClient
      .get<PageResponse<AdminBusinessSummary>>("/admin/businesses", { params: { size: 20, ...params } })
      .then((r) => r.data),
  reviewBusiness: (userId: number, payload: ApprovalDecisionPayload) =>
    apiClient.patch<AdminBusinessSummary>(`/admin/businesses/${userId}/approval`, payload).then((r) => r.data),

  listCampaigns: (params: AdminListParams) =>
    apiClient
      .get<PageResponse<AdminCampaignSummary>>("/admin/campaigns", { params: { size: 20, ...params } })
      .then((r) => r.data),
  reviewCampaign: (id: number, payload: ApprovalDecisionPayload) =>
    apiClient.patch<AdminCampaignSummary>(`/admin/campaigns/${id}/approval`, payload).then((r) => r.data),
  setCampaignDisplayStatus: (id: number, status: CampaignDisplayStatus) =>
    apiClient.patch<AdminCampaignSummary>(`/admin/campaigns/${id}/display-status`, { status }).then((r) => r.data),

  listContent: (params: AdminContentListParams = {}) =>
    apiClient
      .get<PageResponse<AdminContentSummary>>("/admin/content", { params: { size: 20, ...params } })
      .then((r) => r.data),
  reviewContentLink: (id: number, payload: ApprovalDecisionPayload) =>
    apiClient.patch<AdminContentSummary>(`/admin/content/${id}/link-review`, payload).then((r) => r.data),
};
