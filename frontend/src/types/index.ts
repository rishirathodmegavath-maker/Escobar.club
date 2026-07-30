export type UserRole = "CREATOR" | "BUSINESS" | "ADMIN";

export type ContentStatus = "DRAFT" | "SUBMITTED" | "CHANGES_REQUESTED" | "APPROVED" | "REJECTED" | "PUBLISHED";

export type MediaType = "IMAGE" | "VIDEO";

export type CampaignStatus = "DRAFT" | "STARTING_SOON" | "ACTIVE" | "CLOSED";

export type KycStatus = "PENDING" | "VERIFIED" | "REJECTED";

export type PayoutStatus = "BELOW_THRESHOLD" | "PENDING_KYC" | "PAYABLE" | "PAID";

export interface UserSummary {
  id: number;
  email: string;
  role: UserRole;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: UserSummary;
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
  startDate: string;
  endDate: string;
  ratePerThousandViewsInr: number;
  status: CampaignStatus;
  acceptingSubmissions: boolean;
  urgent: boolean;
  hot: boolean;
  createdAt: string;
  updatedAt: string;
}

export type CampaignCategory = "HOT" | "LIVE" | "UPCOMING";

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
  documentUrl: string;
  status: KycStatus;
  reviewNote: string | null;
  reviewedAt: string | null;
}

export interface CreatorKycReviewDetail {
  creatorId: number;
  panNumber: string;
  nameOnPan: string;
  documentUrl: string;
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

export interface ApiErrorShape {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: { field: string; message: string }[];
}
