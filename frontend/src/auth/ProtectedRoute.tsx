import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";
import type { UserRole } from "@/types";

interface ProtectedRouteProps {
  children: ReactNode;
  allowedRoles?: UserRole[];
  loginPath?: string;
}

export function ProtectedRoute({ children, allowedRoles, loginPath = "/login" }: ProtectedRouteProps) {
  const { user, isReady } = useAuth();
  const location = useLocation();

  if (!isReady) return null;

  if (!user) {
    return <Navigate to={loginPath} replace state={{ from: location }} />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
