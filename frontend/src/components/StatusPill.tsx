import clsx from "clsx";
import type { CampaignStatus, ContentStatus, KycStatus, PayoutStatus } from "@/types";

type Status = ContentStatus | CampaignStatus | KycStatus | PayoutStatus;

// Note: PUBLISHED is shared between ContentStatus (a piece of content is live) and CampaignStatus
// (internal "governed by dates" sentinel that's never actually sent to the client - see
// CampaignMapper). Its styling below reflects the ContentStatus meaning, the only one ever rendered.
const styles: Record<Status, string> = {
  PENDING: "bg-gold-soft text-gold-deep",
  SUBMITTED: "bg-gold-soft text-gold-deep",
  CHANGES_REQUESTED: "bg-gold-soft text-gold-deep",
  DRAFT: "bg-ink-100 text-ink-500",
  UPCOMING: "bg-gold-soft text-gold-deep",
  APPROVED: "bg-signal-soft text-signal-deep",
  REJECTED: "bg-danger-soft text-danger-deep",
  PUBLISHED: "bg-ink-900 text-paper-50",
  LIVE: "bg-signal-soft text-signal-deep",
  COMPLETED: "bg-ink-100 text-ink-500",
  CANCELLED: "bg-danger-soft text-danger-deep",
  VERIFIED: "bg-signal-soft text-signal-deep",
  BELOW_THRESHOLD: "bg-ink-100 text-ink-500",
  PENDING_KYC: "bg-gold-soft text-gold-deep",
  PAYABLE: "bg-signal-soft text-signal-deep",
  PAID: "bg-signal-soft text-signal-deep",
};

const labels: Record<Status, string> = {
  PENDING: "Pending",
  SUBMITTED: "Submitted",
  CHANGES_REQUESTED: "Changes requested",
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
};

const dotStyles: Record<Status, string> = {
  PENDING: "bg-gold-500",
  SUBMITTED: "bg-gold-500",
  CHANGES_REQUESTED: "bg-gold-500",
  DRAFT: "bg-ink-300",
  UPCOMING: "bg-gold-500",
  APPROVED: "bg-signal-500",
  REJECTED: "bg-danger-500",
  PUBLISHED: "bg-gold-400",
  LIVE: "bg-signal-500",
  COMPLETED: "bg-ink-300",
  CANCELLED: "bg-danger-500",
  VERIFIED: "bg-signal-500",
  BELOW_THRESHOLD: "bg-ink-300",
  PENDING_KYC: "bg-gold-500",
  PAYABLE: "bg-signal-500",
  PAID: "bg-signal-500",
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
