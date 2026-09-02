import { apiClient } from "./client";
import type { BusinessDashboardSummary, BusinessProfile, PageResponse } from "@/types";

export interface BusinessSearchParams {
  search?: string;
  industry?: string;
  page?: number;
  size?: number;
}

export interface BusinessUpdatePayload {
  companyName: string;
  gstNumber: string;
  contactPersonName: string;
  mobileNumber: string;
  industry: string;
  description: string;
  logoUrl: string;
  website: string;
}

export interface LogoUploadResult {
  url: string;
  contentType: string;
  sizeBytes: number;
}

export const businessesApi = {
  search: (params: BusinessSearchParams) =>
    apiClient
      .get<PageResponse<BusinessProfile>>("/businesses", { params: { size: 12, ...params } })
      .then((r) => r.data),
  getById: (id: number) => apiClient.get<BusinessProfile>(`/businesses/${id}`).then((r) => r.data),
  getMine: () => apiClient.get<BusinessProfile>("/businesses/me").then((r) => r.data),
  dashboard: () => apiClient.get<BusinessDashboardSummary>("/businesses/me/dashboard").then((r) => r.data),
  updateMine: (payload: BusinessUpdatePayload) =>
    apiClient.put<BusinessProfile>("/businesses/me", payload).then((r) => r.data),
  uploadLogo: (file: File, onProgress?: (percent: number) => void) => {
    const formData = new FormData();
    formData.append("file", file);
    return apiClient
      .post<LogoUploadResult>("/media/upload", formData, {
        headers: { "Content-Type": "multipart/form-data" },
        onUploadProgress: (evt) => {
          if (onProgress && evt.total) onProgress(Math.round((evt.loaded / evt.total) * 100));
        },
      })
      .then((r) => r.data);
  },
};
