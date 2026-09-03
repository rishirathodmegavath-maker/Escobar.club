import { useState } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "@/components/AppShell";
import { SplashScreen } from "@/components/SplashScreen";
import { ProtectedRoute } from "@/auth/ProtectedRoute";
import { useAuth } from "@/auth/AuthContext";
import { DiscoverCampaignsPage } from "@/pages/DiscoverCampaignsPage";
import { CampaignDetailPage } from "@/pages/CampaignDetailPage";
import { LoginPage } from "@/pages/LoginPage";
import { RegisterPage } from "@/pages/RegisterPage";
import { ForgotPasswordPage } from "@/pages/ForgotPasswordPage";
import { ResetPasswordPage } from "@/pages/ResetPasswordPage";
import { VerifyEmailPage } from "@/pages/VerifyEmailPage";
import { CreatorHelpPage } from "@/pages/CreatorHelpPage";
import { CreatorDashboardPage } from "@/pages/creator/CreatorDashboardPage";
import { CreatorPayoutsPage } from "@/pages/creator/CreatorPayoutsPage";
import { CreatorContentPage } from "@/pages/creator/CreatorContentPage";
import { CreatorProfilePage } from "@/pages/creator/CreatorProfilePage";
import { CreatorProfileViewPage } from "@/pages/creator/CreatorProfileViewPage";
import { CreatorKycPage } from "@/pages/creator/CreatorKycPage";
import { BusinessDashboardPage } from "@/pages/business/BusinessDashboardPage";
import { BusinessContentReviewPage } from "@/pages/business/BusinessContentReviewPage";
import { BusinessPayoutsPage } from "@/pages/business/BusinessPayoutsPage";
import { BusinessCreatorProfilePage } from "@/pages/business/BusinessCreatorProfilePage";
import { BusinessProfilePage } from "@/pages/business/BusinessProfilePage";
import { BusinessProfileViewPage } from "@/pages/business/BusinessProfileViewPage";
import { BusinessLeaderboardPage } from "@/pages/business/BusinessLeaderboardPage";
import { BusinessCampaignsPage } from "@/pages/business/BusinessCampaignsPage";
import { LeaderboardPage } from "@/pages/LeaderboardPage";
import { CreatorPublicProfilePage } from "@/pages/CreatorPublicProfilePage";
import { SecuritySettingsPage } from "@/pages/SecuritySettingsPage";
import { AdminLoginPage } from "@/pages/admin/AdminLoginPage";
import { AdminDashboardPage } from "@/pages/admin/AdminDashboardPage";
import { AdminCreatorsPage } from "@/pages/admin/AdminCreatorsPage";
import { AdminBusinessesPage } from "@/pages/admin/AdminBusinessesPage";
import { AdminCampaignsPage } from "@/pages/admin/AdminCampaignsPage";
import { AdminContentPage } from "@/pages/admin/AdminContentPage";
import { NotFoundPage } from "@/pages/NotFoundPage";

export default function App() {
  const { isReady } = useAuth();
  const [splashDone, setSplashDone] = useState(false);

  if (!splashDone) {
    return <SplashScreen ready={isReady} onDone={() => setSplashDone(true)} />;
  }

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route path="/help" element={<CreatorHelpPage />} />
      <Route path="/admin/login" element={<AdminLoginPage />} />

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
        path="/creator/dashboard"
        element={
          <ProtectedRoute allowedRoles={["CREATOR"]}>
            <AppShell>
              <CreatorDashboardPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/creator/payouts"
        element={
          <ProtectedRoute allowedRoles={["CREATOR"]}>
            <AppShell>
              <CreatorPayoutsPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
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
              <CreatorProfileViewPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/creator/profile/edit"
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
        path="/business/dashboard"
        element={
          <ProtectedRoute allowedRoles={["BUSINESS"]}>
            <AppShell>
              <BusinessDashboardPage />
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
        path="/business/payouts"
        element={
          <ProtectedRoute allowedRoles={["BUSINESS"]}>
            <AppShell>
              <BusinessPayoutsPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/business/creators/:creatorId"
        element={
          <ProtectedRoute allowedRoles={["BUSINESS"]}>
            <AppShell>
              <BusinessCreatorProfilePage />
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
              <BusinessProfileViewPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/business/profile/edit"
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
      <Route
        path="/creators/:creatorId"
        element={
          <ProtectedRoute allowedRoles={["CREATOR", "BUSINESS"]}>
            <AppShell>
              <CreatorPublicProfilePage />
            </AppShell>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin"
        element={
          <ProtectedRoute allowedRoles={["ADMIN"]} loginPath="/admin/login">
            <AppShell>
              <AdminDashboardPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/creators"
        element={
          <ProtectedRoute allowedRoles={["ADMIN"]} loginPath="/admin/login">
            <AppShell>
              <AdminCreatorsPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/businesses"
        element={
          <ProtectedRoute allowedRoles={["ADMIN"]} loginPath="/admin/login">
            <AppShell>
              <AdminBusinessesPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/campaigns"
        element={
          <ProtectedRoute allowedRoles={["ADMIN"]} loginPath="/admin/login">
            <AppShell>
              <AdminCampaignsPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/content"
        element={
          <ProtectedRoute allowedRoles={["ADMIN"]} loginPath="/admin/login">
            <AppShell>
              <AdminContentPage />
            </AppShell>
          </ProtectedRoute>
        }
      />

      <Route
        path="/account/security"
        element={
          <ProtectedRoute allowedRoles={["CREATOR", "BUSINESS"]}>
            <AppShell>
              <SecuritySettingsPage />
            </AppShell>
          </ProtectedRoute>
        }
      />

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
