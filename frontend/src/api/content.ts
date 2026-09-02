import { apiClient } from "./client";
import type { ContentRecord, ContentStatus, MediaType, PageResponse } from "@/types";

export interface SubmitContentPayload {
  caption: string;
  mediaUrl: string;
  mediaType: MediaType;
}

export interface ReviewContentPayload {
  decision: ContentStatus;
  note?: string;
}

export interface BulkReviewFailure {
  contentId: number;
  reason: string;
}

export interface BulkReviewResult {
  succeeded: number;
  failures: BulkReviewFailure[];
}

export interface UploadResult {
  url: string;
  contentType: string;
  sizeBytes: number;
}

export const contentApi = {
  submit: (campaignId: number, payload: SubmitContentPayload) =>
    apiClient
      .post<ContentRecord>(`/campaigns/${campaignId}/content`, {
        campaignId,
        ...payload,
      })
      .then((r) => r.data),
  resubmit: (contentId: number, payload: SubmitContentPayload) =>
    apiClient.patch<ContentRecord>(`/content/${contentId}`, payload).then((r) => r.data),
  review: (contentId: number, payload: ReviewContentPayload) =>
    apiClient.patch<ContentRecord>(`/content/${contentId}/review`, payload).then((r) => r.data),
  reviewBulk: (contentIds: number[], payload: ReviewContentPayload) =>
    apiClient
      .patch<BulkReviewResult>("/content/review/bulk", { contentIds, ...payload })
      .then((r) => r.data),
  publish: (contentId: number, postUrl: string) =>
    apiClient.patch<ContentRecord>(`/content/${contentId}/publish`, { postUrl }).then((r) => r.data),
  reviewQueue: (businessId: number, status?: ContentStatus, page = 0, size = 10) =>
    apiClient
      .get<PageResponse<ContentRecord>>(`/businesses/${businessId}/content`, {
        params: { status, page, size },
      })
      .then((r) => r.data),
  mine: (page = 0, size = 10) =>
    apiClient.get<PageResponse<ContentRecord>>("/content/me", { params: { page, size } }).then((r) => r.data),
  upload: (file: File, onProgress?: (percent: number) => void) => {
    const formData = new FormData();
    formData.append("file", file);
    return apiClient
      .post<UploadResult>("/media/upload", formData, {
        headers: { "Content-Type": "multipart/form-data" },
        onUploadProgress: (evt) => {
          if (onProgress && evt.total) onProgress(Math.round((evt.loaded / evt.total) * 100));
        },
      })
      .then((r) => r.data);
  },
};
