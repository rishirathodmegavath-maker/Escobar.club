import { apiClient } from "./client";

export interface FormDraft<T> {
  data: T;
  updatedAt: string;
}

export const draftsApi = {
  get: async <T,>(key: string): Promise<FormDraft<T> | null> => {
    const res = await apiClient.get<{ draftKey: string; payload: string; updatedAt: string } | null>(
      `/drafts/${key}`,
      { validateStatus: (status) => status === 200 || status === 204 },
    );
    if (res.status === 204 || !res.data) return null;
    return { data: JSON.parse(res.data.payload) as T, updatedAt: res.data.updatedAt };
  },
  save: <T,>(key: string, data: T) => apiClient.put(`/drafts/${key}`, { payload: JSON.stringify(data) }),
  remove: (key: string) => apiClient.delete(`/drafts/${key}`),
};
