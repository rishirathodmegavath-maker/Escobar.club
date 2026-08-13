export type UserRole = "CREATOR" | "BUSINESS" | "ADMIN";

export type ContentStatus = "DRAFT" | "SUBMITTED" | "CHANGES_REQUESTED" | "APPROVED" | "REJECTED" | "PUBLISHED";

export type MediaType = "IMAGE" | "VIDEO";

export type CampaignStatus = "DRAFT" | "PUBLISHED" | "UPCOMING" | "LIVE" | "COMPLETED" | "CANCELLED";

// Subset of CampaignStatus a business may pick manually when editing a campaign. UPCOMING/LIVE/COMPLETED
// are always computed from dates and are never valid here.
export type ManualCampaignStatus = "DRAFT" | "PUBLISHED" | "CANCELLED";

export type KycStatus = "PENDING" | "VERIFIED" | "REJECTED";

export type PayoutStatus = "BELOW_THRESHOLD" | "PENDING_KYC" | "PAYABLE" | "PAID";

export type ApprovalStatus = "PENDING" | "APPROVED" | "REJECTED";

// Admin-curated label shown to creators, independent of the auto-computed CampaignStatus above.
export type CampaignDisplayStatus = "UPCOMING" | "LIVE" | "HOT" | "CLOSED";

export interface UserSummary {
  id: number;
  email: string;
  role: UserRole;
  hasPassword: boolean;
  twoFactorEnabled: boolean;
}

export interface AuthResponse {
  accessToken: string;
  sessionId: number;
  user: UserSummary;
}

export interface LoginResponse {
  requiresTwoFactor: boolean;
  challengeToken: string | null;
  auth: AuthResponse | null;
}

export type LoginResult = { status: "success"; user: UserSummary } | { status: "twoFactorRequired"; challengeToken: string };

export interface TwoFactorSetupResponse {
  secret: string;
  otpauthUri: string;
}

export interface SessionSummary {
  id: number;
  ipAddress: string | null;
  userAgent: string | null;
  createdAt: string;
  lastUsedAt: string | null;
  expiresAt: string;
}

export interface CreatorProfile {
  id: number;
  userId: number;
  email: string;
  displayName: string;
  bio: string | null;
  profilePictureUrl: string | null;
  niche: string | null;
  openToOtherNiches: boolean;
  instagramProfileUrl: string;
  followerCount: number;
  portfolioLinks: string[];
  createdAt: string;
}

export interface BusinessProfile {
  id: number;
  userId: number;
  companyName: string;
  gstNumber: string;
  contactPersonName: string;
  mobileNumber: string;
  industry: string | null;
  description: string | null;
  logoUrl: string | null;
  website: string | null;
  createdAt: string;
}

export interface Campaign {
  id: number;
  businessId: number;
  businessCompanyName: string;
  businessLogoUrl: string | null;
  title: string;
  description: string | null;
  submissionOpenAt: string;
  submissionDeadline: string;
  publishStartAt: string;
  publishEndAt: string;
  ratePerThousandViewsInr: number;
  status: CampaignStatus;
  acceptingSubmissions: boolean;
  urgent: boolean;
  hot: boolean;
  approvalStatus: ApprovalStatus;
  adminDisplayStatus: CampaignDisplayStatus | null;
  createdAt: string;
  updatedAt: string;
}

export type CampaignCategory = "HOT" | "LIVE" | "UPCOMING" | "COMPLETED";

export interface ContentReviewNote {
  id: number;
  authoredByUserId: number;
  contentVersion: number;
  decision: ContentStatus;
  noteText: string | null;
  createdAt: string;
}

export interface ContentRecord {
  id: number;
  creatorId: number;
  creatorDisplayName: string;
  creatorProfilePictureUrl: string | null;
  campaignId: number;
  campaignTitle: string;
  businessId: number;
  businessCompanyName: string;
  businessLogoUrl: string | null;
  caption: string | null;
  mediaUrl: string;
  mediaType: MediaType;
  postUrl: string | null;
  status: ContentStatus;
  version: number;
  reviewNotes: ContentReviewNote[];
  createdAt: string;
  updatedAt: string;
  submittedAt: string | null;
  publishedAt: string | null;
}

export interface ContentMetricsSnapshot {
  id: number;
  contentId: number;
  likeCount: number;
  commentCount: number;
  viewCount: number | null;
  fetchedAt: string;
}

export interface CreatorKycProfile {
  creatorId: number;
  panNumberMasked: string;
  nameOnPan: string;
  hasDocument: boolean;
  status: KycStatus;
  reviewNote: string | null;
  reviewedAt: string | null;
}

export interface CreatorKycReviewDetail {
  creatorId: number;
  panNumber: string;
  nameOnPan: string;
  hasDocument: boolean;
  status: KycStatus;
  reviewNote: string | null;
  reviewedAt: string | null;
}

export interface Payout {
  id: number;
  contentId: number;
  creatorId: number;
  campaignId: number;
  businessId: number;
  viewCountUsed: number;
  rateUsed: number;
  amountInr: number;
  status: PayoutStatus;
  calculatedAt: string;
  eligibleAt: string | null;
  paidAt: string | null;
  paidNote: string | null;
}

export interface LeaderboardEntry {
  rank: number;
  creatorId: number;
  creatorDisplayName: string;
  creatorProfilePictureUrl: string | null;
  totalViews: number;
  publishedContentCount: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface AdminDashboardSummary {
  totalBrands: number;
  totalCreators: number;
  totalCampaigns: number;
  pendingCampaignApprovals: number;
  pendingCreatorKyc: number;
}

export interface AdminCreatorSummary {
  userId: number;
  email: string;
  displayName: string | null;
  active: boolean;
  kycStatus: KycStatus | null;
  createdAt: string;
}

export interface AdminBusinessSummary {
  userId: number;
  email: string;
  companyName: string;
  gstNumber: string;
  contactPersonName: string;
  approvalStatus: ApprovalStatus;
  createdAt: string;
}

export interface AdminCampaignSummary {
  id: number;
  title: string;
  businessCompanyName: string | null;
  status: CampaignStatus;
  approvalStatus: ApprovalStatus;
  adminDisplayStatus: CampaignDisplayStatus | null;
  publishStartAt: string;
  publishEndAt: string;
  createdAt: string;
}

export interface AdminContentSummary {
  id: number;
  creatorDisplayName: string | null;
  creatorEmail: string;
  businessCompanyName: string | null;
  campaignTitle: string;
  status: ContentStatus;
  postUrl: string | null;
  publishedAt: string | null;
  likeCount: number | null;
  commentCount: number | null;
  viewCount: number | null;
  metricsLastSyncedAt: string | null;
}

export interface ApiErrorShape {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: { field: string; message: string }[];
}
