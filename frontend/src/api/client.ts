import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import type { AuthResponse } from "@/types";

const ACCESS_TOKEN_KEY = "escobar.accessToken";
const REFRESH_TOKEN_KEY = "escobar.refreshToken";
const SESSION_ID_KEY = "escobar.sessionId";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) || "/api";

export const tokenStorage = {
  getAccessToken: () => localStorage.getItem(ACCESS_TOKEN_KEY),
  getRefreshToken: () => localStorage.getItem(REFRESH_TOKEN_KEY),
  getSessionId: () => {
    const raw = localStorage.getItem(SESSION_ID_KEY);
    return raw ? Number(raw) : null;
  },
  setTokens: (accessToken: string, refreshToken: string, sessionId?: number) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    if (sessionId !== undefined) {
      localStorage.setItem(SESSION_ID_KEY, String(sessionId));
    }
  },
  clear: () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(SESSION_ID_KEY);
  },
};

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { "Content-Type": "application/json" },
});

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStorage.getAccessToken();
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

type UnauthorizedListener = () => void;
let unauthorizedListener: UnauthorizedListener | null = null;
export const onUnauthorized = (listener: UnauthorizedListener) => {
  unauthorizedListener = listener;
};

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) return null;

  try {
    const response = await axios.post<AuthResponse>(`${API_BASE_URL}/auth/refresh`, { refreshToken });
    tokenStorage.setTokens(response.data.accessToken, response.data.refreshToken, response.data.sessionId);
    return response.data.accessToken;
  } catch {
    return null;
  }
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      originalRequest._retry = true;

      refreshPromise ??= refreshAccessToken().finally(() => {
        refreshPromise = null;
      });
      const newToken = await refreshPromise;

      if (newToken) {
        originalRequest.headers = originalRequest.headers ?? {};
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(originalRequest);
      }

      tokenStorage.clear();
      unauthorizedListener?.();
    }

    return Promise.reject(error);
  },
);

export function extractErrorMessage(error: unknown, fallback = "Something went wrong. Please try again."): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | undefined;
    if (data?.message) return data.message;
  }
  return fallback;
}
