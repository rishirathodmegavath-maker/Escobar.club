import { apiClient } from "./client";
import type { CreatorProfile } from "@/types";

export interface CreatorUpdatePayload {
  displayName: string;
  bio: string;
  profilePictureUrl: string;
  niche: string;
  openToOtherNiches: boolean;
  instagramProfileUrl: string;
  followerCount: number;
  portfolioLinks: string[];
}

export interface ProfilePictureUploadResult {
  url: string;
  contentType: string;
  sizeBytes: number;
}

export const creatorsApi = {
  getById: (id: number) => apiClient.get<CreatorProfile>(`/creators/${id}`).then((r) => r.data),
  getMine: () => apiClient.get<CreatorProfile>("/creators/me").then((r) => r.data),
  updateMine: (payload: CreatorUpdatePayload) =>
    apiClient.put<CreatorProfile>("/creators/me", payload).then((r) => r.data),
  uploadProfilePicture: (file: File, onProgress?: (percent: number) => void) => {
    const formData = new FormData();
    formData.append("file", file);
    return apiClient
      .post<ProfilePictureUploadResult>("/media/upload", formData, {
        headers: { "Content-Type": "multipart/form-data" },
        onUploadProgress: (evt) => {
          if (onProgress && evt.total) onProgress(Math.round((evt.loaded / evt.total) * 100));
        },
      })
      .then((r) => r.data);
  },
};
