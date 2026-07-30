import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "@/components/AppShell";
import { ProtectedRoute } from "@/auth/ProtectedRoute";
import { useAuth } from "@/auth/AuthContext";
import { DiscoverCampaignsPage } from "@/pages/DiscoverCampaignsPage";
import { CampaignDetailPage } from "@/pages/CampaignDetailPage";
import { LoginPage } from "@/pages/LoginPage";
import { RegisterPage } from "@/pages/RegisterPage";
import { CreatorHelpPage } from "@/pages/CreatorHelpPage";
import { CreatorContentPage } from "@/pages/creator/CreatorContentPage";
import { CreatorProfilePage } from "@/pages/creator/CreatorProfilePage";
import { CreatorKycPage } from "@/pages/creator/CreatorKycPage";
import { BusinessContentReviewPage } from "@/pages/business/BusinessContentReviewPage";
import { BusinessProfilePage } from "@/pages/business/BusinessProfilePage";
import { BusinessLeaderboardPage } from "@/pages/business/BusinessLeaderboardPage";
import { BusinessCampaignsPage } from "@/pages/business/BusinessCampaignsPage";
import { LeaderboardPage } from "@/pages/LeaderboardPage";

export default function App() {
  const { isReady } = useAuth();

  if (!isReady) return null;

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/help" element={<CreatorHelpPage />} />

      <Route
        path="/"
        element={
          <AppShell>
            <DiscoverCampaignsPage />
          </AppShell>
        }
      />
      <Route
        path="/campaigns/:id"
        element={
          <AppShell>
            <CampaignDetailPage />
          </AppShell>
        }
      />
      <Route path="/businesses/:id" element={<Navigate to="/" replace />} />

      <Route
        path="/creator/content"
        element={
          <ProtectedRoute allowedRoles={["CREATOR"]}>
            <AppShell>
              <CreatorContentPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/creator/profile"
        element={
          <ProtectedRoute allowedRoles={["CREATOR"]}>
            <AppShell>
              <CreatorProfilePage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/creator/kyc"
        element={
          <ProtectedRoute allowedRoles={["CREATOR"]}>
            <AppShell>
              <CreatorKycPage />
            </AppShell>
          </ProtectedRoute>
        }
      />

      <Route
        path="/business/content"
        element={
          <ProtectedRoute allowedRoles={["BUSINESS"]}>
            <AppShell>
              <BusinessContentReviewPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/business/campaigns"
        element={
          <ProtectedRoute allowedRoles={["BUSINESS"]}>
            <AppShell>
              <BusinessCampaignsPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/business/profile"
        element={
          <ProtectedRoute allowedRoles={["BUSINESS"]}>
            <AppShell>
              <BusinessProfilePage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/business/leaderboard"
        element={
          <ProtectedRoute allowedRoles={["BUSINESS"]}>
            <AppShell>
              <BusinessLeaderboardPage />
            </AppShell>
          </ProtectedRoute>
        }
      />

      <Route
        path="/leaderboard"
        element={
          <ProtectedRoute allowedRoles={["CREATOR", "BUSINESS"]}>
            <AppShell>
              <LeaderboardPage />
            </AppShell>
          </ProtectedRoute>
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
