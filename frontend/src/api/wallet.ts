import { apiClient } from "./client";
import type { PageResponse, WalletSummary, WalletTransaction, WalletTransactionStatus, WalletTransactionType } from "@/types";

export interface WalletTransactionFilters {
  type?: WalletTransactionType;
  status?: WalletTransactionStatus;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export interface AddMoneyPayload {
  amountInr: number;
  note?: string;
}

export const businessWalletApi = {
  getSummary: (businessId: number) => apiClient.get<WalletSummary>(`/businesses/${businessId}/wallet`).then((r) => r.data),
  listTransactions: (businessId: number, filters: WalletTransactionFilters = {}) =>
    apiClient
      .get<PageResponse<WalletTransaction>>(`/businesses/${businessId}/wallet/transactions`, { params: { size: 20, ...filters } })
      .then((r) => r.data),
  addMoney: (businessId: number, payload: AddMoneyPayload) =>
    apiClient.post<WalletTransaction>(`/businesses/${businessId}/wallet/transactions`, payload).then((r) => r.data),
};
