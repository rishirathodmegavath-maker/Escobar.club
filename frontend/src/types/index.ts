export type UserRole = "CREATOR" | "BUSINESS" | "ADMIN";

export type ContentStatus =
  | "DRAFT"
  | "SUBMITTED"
  | "CHANGES_REQUESTED"
  | "APPROVED"
  | "REJECTED"
  | "PENDING_LINK_REVIEW"
  | "PUBLISHED";

export type MediaType = "IMAGE" | "VIDEO";

export type CampaignStatus = "DRAFT" | "PUBLISHED" | "UPCOMING" | "LIVE" | "COMPLETED" | "CANCELLED";

// Subset of CampaignStatus a business may pick manually when editing a campaign. UPCOMING/LIVE/COMPLETED
// are always computed from dates and are never valid here.
export type ManualCampaignStatus = "DRAFT" | "PUBLISHED" | "CANCELLED";

export type KycStatus = "PENDING" | "VERIFIED" | "REJECTED";

export type PayoutStatus = "BELOW_THRESHOLD" | "PENDING_KYC" | "PAYABLE" | "PAID";

export type ApprovalStatus = "PENDING" | "APPROVED" | "REJECTED";

export type WalletTransactionType = "CREDIT" | "DEBIT";

export type WalletTransactionStatus = "PENDING" | "CONFIRMED" | "REJECTED" | "REVERSED";

export type FundingSource = "BUSINESSMAN_MANUAL" | "ADMIN_MANUAL" | "CAMPAIGN_PAYMENT" | "REVERSAL";

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
  // Null means no cap set (unlimited). Both are only populated for the owning business - a public
  // listing (Discover, campaign detail) never exposes a business's internal budget numbers, though
  // acceptingSubmissions above still correctly reflects a budget-exhausted campaign either way.
  maxBudgetInr: number | null;
  committedBudgetInr: number | null;
  // Human-readable reason submissions are closed (date window, or the budget cap) - null while
  // accepting. Unlike the budget fields above, safe on every endpoint, public listing included.
  submissionClosedReason: string | null;
  approvalStatus: ApprovalStatus;
  adminDisplayStatus: CampaignDisplayStatus | null;
  // Computed server-side (true only for DRAFT/UPCOMING) - always gate the "Change Schedule" action
  // on this field rather than re-deriving the rule from `status`.
  canChangeSchedule: boolean;
  createdAt: string;
  updatedAt: string;
  // Sum of the latest metrics snapshot's viewCount across this campaign's PUBLISHED content. Only
  // populated on Business > My Campaigns (campaignsApi.mine) - null everywhere else, same scoping
  // as the budget fields above.
  totalViews: number | null;
}

export interface CampaignScheduleChange {
  id: number;
  oldSubmissionOpenAt: string;
  oldSubmissionDeadline: string;
  oldPublishStartAt: string;
  oldPublishEndAt: string;
  newSubmissionOpenAt: string;
  newSubmissionDeadline: string;
  newPublishStartAt: string;
  newPublishEndAt: string;
  changedByUserId: number;
  changedAt: string;
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
  // True only once an admin has verified this creator's KYC - a business's own peer review
  // (status can also be VERIFIED that way) is not sufficient to unlock campaign participation.
  eligibleToParticipate: boolean;
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
  creatorDisplayName: string | null;
  campaignId: number;
  campaignTitle: string;
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

export interface WalletSummary {
  businessId: number;
  businessName: string | null;
  availableBalanceInr: number;
  totalAddedInr: number;
  totalPaidInr: number;
  lastActivityAt: string | null;
}

export interface WalletTransaction {
  id: number;
  businessId: number;
  businessName: string | null;
  type: WalletTransactionType;
  status: WalletTransactionStatus;
  fundingSource: FundingSource;
  amountInr: number;
  note: string | null;
  performedByUserId: number;
  performedByName: string | null;
  payoutId: number | null;
  campaignTitle: string | null;
  reversedTransactionId: number | null;
  createdAt: string;
  confirmedAt: string | null;
  confirmedByName: string | null;
}

export interface AdminDashboardSummary {
  totalBrands: number;
  totalCreators: number;
  totalCampaigns: number;
  pendingCampaignApprovals: number;
  pendingCreatorKyc: number;
  totalFundsHeldInr: number;
  totalPaidInr: number;
  totalAvailableInr: number;
  activeWalletsCount: number;
  pendingTopUpsCount: number;
  recentWalletActivity: WalletTransaction[];
}

export interface PerformanceWindow {
  views: number;
  likes: number;
  comments: number;
  publishedCount: number;
  engagementRate: number;
}

export interface PerformanceSummary {
  sevenDay: PerformanceWindow;
  thirtyDay: PerformanceWindow;
  ninetyDay: PerformanceWindow;
  allTime: PerformanceWindow;
}

export interface NeedsAttentionItem {
  message: string;
  actionLabel: string;
  actionPath: string;
}

export interface TopContentItem {
  contentId: number;
  creatorDisplayName: string;
  campaignTitle: string;
  mediaType: string;
  views: number;
  likes: number;
  comments: number;
  engagementRate: number;
  postUrl: string | null;
}

export interface BusinessDashboardSummary {
  approvalStatus: ApprovalStatus;
  totalCampaigns: number;
  liveCampaigns: number;
  campaignsPendingApproval: number;
  contentAwaitingReview: number;
  contentChangesRequested: number;
  contentPendingLinkReview: number;
  publishedContentCount: number;
  payoutsPayableCount: number;
  payoutsPayableAmountInr: number;
  payoutsPendingKycCount: number;
  totalPaidOutInr: number;
  campaignsNearBudgetCap: number;
  approvedContentCount: number;
  rejectedContentCount: number;
  participatingCreatorsCount: number;
  totalViews: number;
  totalEngagement: number;
  totalBudgetInr: number;
  totalCommittedInr: number;
  totalRemainingInr: number;
  needsAttention: NeedsAttentionItem[];
  campaignsPreview: CampaignPreview[];
  creatorActivity: CreatorActivityItem[];
  topContent: TopContentItem[];
  performance: PerformanceSummary;
}

export interface CampaignPreview {
  campaignId: number;
  title: string;
  status: string;
  creatorsCount: number;
  contentSubmittedCount: number;
  contentPublishedCount: number;
  views: number;
  maxBudgetInr: number | null;
  committedBudgetInr: number;
  submissionDeadline: string;
}

export interface CreatorActivityItem {
  creatorDisplayName: string;
  campaignTitle: string;
  message: string;
  occurredAt: string;
}

export interface CampaignAnalytics {
  campaignId: number;
  views: number;
  likes: number;
  comments: number;
  engagementRate: number;
  creatorsCount: number;
  publishedContentCount: number;
  budgetCommittedInr: number;
}

export interface ContentStatusCounts {
  submitted: number;
  changesRequested: number;
  approved: number;
  pendingLinkReview: number;
  published: number;
  rejected: number;
}

export interface EarningsSummary {
  pendingKycInr: number;
  payableInr: number;
  paidInr: number;
  thisMonthPaidInr: number;
}

export interface ActiveCampaignSummary {
  campaignId: number;
  title: string;
  status: string;
  views: number;
  earningsInr: number;
}

export interface RecentActivityItem {
  message: string;
  occurredAt: string;
}

export interface PayoutPreviewItem {
  contentId: number;
  campaignTitle: string;
  amountInr: number;
  status: PayoutStatus;
  paidAt: string | null;
}

export interface CreatorDashboardSummary {
  kycStatus: KycStatus | null;
  profileCompletionPercent: number;
  profileCompletionMissing: string[];
  activeCampaignsCount: number;
  submissionStatus: ContentStatusCounts;
  earnings: EarningsSummary;
  performance: PerformanceSummary;
  needsAttention: NeedsAttentionItem[];
  recommendedCampaigns: Campaign[];
  activeCampaigns: ActiveCampaignSummary[];
  topContent: TopContentItem[];
  recentActivity: RecentActivityItem[];
  recentPayouts: PayoutPreviewItem[];
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
