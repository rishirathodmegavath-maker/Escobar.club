import clsx from "clsx";
import type { ApprovalStatus, CampaignDisplayStatus, CampaignStatus, ContentStatus, KycStatus, PayoutStatus } from "@/types";

type Status = ContentStatus | CampaignStatus | KycStatus | PayoutStatus | ApprovalStatus | CampaignDisplayStatus;

// Note: PUBLISHED is shared between ContentStatus (a piece of content is live) and CampaignStatus
// (internal "governed by dates" sentinel that's never actually sent to the client - see
// CampaignMapper). Its styling below reflects the ContentStatus meaning, the only one ever rendered.
const styles: Record<Status, string> = {
  PENDING: "bg-gold-soft text-gold-deep",
  SUBMITTED: "bg-gold-soft text-gold-deep",
  CHANGES_REQUESTED: "bg-gold-soft text-gold-deep",
  PENDING_LINK_REVIEW: "bg-gold-soft text-gold-deep",
  DRAFT: "bg-ink-100 text-ink-500",
  UPCOMING: "bg-gold-soft text-gold-deep",
  APPROVED: "bg-mint-soft text-mint-deep",
  REJECTED: "bg-danger-soft text-danger-deep",
  PUBLISHED: "bg-mint-soft text-mint-deep",
  LIVE: "bg-mint-soft text-mint-deep",
  COMPLETED: "bg-ink-100 text-ink-500",
  CANCELLED: "bg-danger-soft text-danger-deep",
  VERIFIED: "bg-mint-soft text-mint-deep",
  BELOW_THRESHOLD: "bg-ink-100 text-ink-500",
  PENDING_KYC: "bg-gold-soft text-gold-deep",
  PAYABLE: "bg-mint-soft text-mint-deep",
  PAID: "bg-mint-soft text-mint-deep",
  HOT: "bg-danger-soft text-danger-deep",
  CLOSED: "bg-ink-100 text-ink-500",
};

const labels: Record<Status, string> = {
  PENDING: "Pending",
  SUBMITTED: "Submitted",
  CHANGES_REQUESTED: "Changes requested",
  PENDING_LINK_REVIEW: "Link pending review",
  DRAFT: "Draft",
  UPCOMING: "Upcoming",
  APPROVED: "Approved",
  REJECTED: "Rejected",
  PUBLISHED: "Published",
  LIVE: "Live",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
  VERIFIED: "Verified",
  BELOW_THRESHOLD: "Below threshold",
  PENDING_KYC: "Awaiting KYC",
  PAYABLE: "Payable",
  PAID: "Paid",
  HOT: "Hot",
  CLOSED: "Closed",
};

const dotStyles: Record<Status, string> = {
  PENDING: "bg-gold-500",
  SUBMITTED: "bg-gold-500",
  CHANGES_REQUESTED: "bg-gold-500",
  PENDING_LINK_REVIEW: "bg-gold-500",
  DRAFT: "bg-ink-300",
  UPCOMING: "bg-gold-500",
  APPROVED: "bg-mint-500",
  REJECTED: "bg-danger-500",
  PUBLISHED: "bg-mint-500",
  LIVE: "bg-mint-500",
  COMPLETED: "bg-ink-300",
  CANCELLED: "bg-danger-500",
  VERIFIED: "bg-mint-500",
  BELOW_THRESHOLD: "bg-ink-300",
  PENDING_KYC: "bg-gold-500",
  PAYABLE: "bg-mint-500",
  PAID: "bg-mint-500",
  HOT: "bg-danger-500",
  CLOSED: "bg-ink-300",
};

export function StatusPill({ status, className }: { status: Status; className?: string }) {
  return (
    <span
      className={clsx(
        "inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 font-mono text-xs font-semibold uppercase tracking-wide",
        styles[status],
        className,
      )}
    >
      <span className={clsx("h-1.5 w-1.5 rounded-full", dotStyles[status])} />
      {labels[status]}
    </span>
  );
}
