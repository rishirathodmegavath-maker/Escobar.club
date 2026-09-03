import { apiClient } from "./client";
import type { PageResponse, Payout, PayoutStatus } from "@/types";

export const payoutsApi = {
  getForContent: (contentId: number) =>
    apiClient.get<Payout>(`/content/${contentId}/payout`).then((r) => r.data),
  listForBusiness: (businessId: number, status?: PayoutStatus, page = 0, size = 20) =>
    apiClient
      .get<PageResponse<Payout>>(`/businesses/${businessId}/payouts`, { params: { status, page, size } })
      .then((r) => r.data),
  listForCreator: (creatorId: number, status?: PayoutStatus, page = 0, size = 20) =>
    apiClient
      .get<PageResponse<Payout>>(`/creators/${creatorId}/payouts`, { params: { status, page, size } })
      .then((r) => r.data),
  markPaid: (contentId: number, paidNote?: string) =>
    apiClient.patch<Payout>(`/content/${contentId}/payout/paid`, { paidNote }).then((r) => r.data),
  exportCsv: (businessId: number, status?: PayoutStatus) =>
    apiClient
      .get<Blob>(`/businesses/${businessId}/payouts/export`, { params: { status }, responseType: "blob" })
      .then((r) => r.data),
};
