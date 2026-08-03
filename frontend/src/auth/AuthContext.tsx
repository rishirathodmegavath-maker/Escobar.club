import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import {
  authApi,
  type ForgotPasswordPayload,
  type GoogleAuthPayload,
  type LoginPayload,
  type RegisterPayload,
  type RegisterResult,
  type ResendVerificationPayload,
  type ResetPasswordPayload,
  type VerifyEmailPayload,
} from "@/api/auth";
import { accountApi, type SetPasswordPayload } from "@/api/account";
import { onUnauthorized, tokenStorage } from "@/api/client";
import type { UserSummary } from "@/types";

const USER_KEY = "escobar.user";

interface AuthContextValue {
  user: UserSummary | null;
  isAuthenticated: boolean;
  isReady: boolean;
  login: (payload: LoginPayload) => Promise<UserSummary>;
  register: (payload: RegisterPayload) => Promise<RegisterResult>;
  loginWithGoogle: (payload: GoogleAuthPayload) => Promise<UserSummary>;
  forgotPassword: (payload: ForgotPasswordPayload) => Promise<void>;
  resetPassword: (payload: ResetPasswordPayload) => Promise<void>;
  setPassword: (payload: SetPasswordPayload) => Promise<UserSummary>;
  verifyEmail: (payload: VerifyEmailPayload) => Promise<UserSummary>;
  resendVerification: (payload: ResendVerificationPayload) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    const stored = localStorage.getItem(USER_KEY);
    if (stored && tokenStorage.getAccessToken()) {
      setUser(JSON.parse(stored));
    }
    setIsReady(true);
  }, []);

  useEffect(() => {
    onUnauthorized(() => {
      setUser(null);
      localStorage.removeItem(USER_KEY);
    });
  }, []);

  const persist = useCallback((nextUser: UserSummary, accessToken: string, refreshToken: string) => {
    tokenStorage.setTokens(accessToken, refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser));
    setUser(nextUser);
  }, []);

  const login = useCallback(
    async (payload: LoginPayload) => {
      const response = await authApi.login(payload);
      persist(response.user, response.accessToken, response.refreshToken);
      return response.user;
    },
    [persist],
  );

  const register = useCallback(async (payload: RegisterPayload) => authApi.register(payload), []);

  const loginWithGoogle = useCallback(
    async (payload: GoogleAuthPayload) => {
      const response = await authApi.googleAuth(payload);
      persist(response.user, response.accessToken, response.refreshToken);
      return response.user;
    },
    [persist],
  );

  const forgotPassword = useCallback((payload: ForgotPasswordPayload) => authApi.forgotPassword(payload), []);

  const resetPassword = useCallback((payload: ResetPasswordPayload) => authApi.resetPassword(payload), []);

  const setPassword = useCallback(async (payload: SetPasswordPayload) => {
    const nextUser = await accountApi.setPassword(payload);
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser));
    setUser(nextUser);
    return nextUser;
  }, []);

  const verifyEmail = useCallback(
    async (payload: VerifyEmailPayload) => {
      const response = await authApi.verifyEmail(payload);
      persist(response.user, response.accessToken, response.refreshToken);
      return response.user;
    },
    [persist],
  );

  const resendVerification = useCallback(
    (payload: ResendVerificationPayload) => authApi.resendVerification(payload),
    [],
  );

  const logout = useCallback(() => {
    const refreshToken = tokenStorage.getRefreshToken();
    if (refreshToken) {
      authApi.logout(refreshToken).catch(() => undefined);
    }
    tokenStorage.clear();
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: !!user,
      isReady,
      login,
      register,
      loginWithGoogle,
      forgotPassword,
      resetPassword,
      setPassword,
      verifyEmail,
      resendVerification,
      logout,
    }),
    [
      user,
      isReady,
      login,
      register,
      loginWithGoogle,
      forgotPassword,
      resetPassword,
      setPassword,
      verifyEmail,
      resendVerification,
      logout,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
