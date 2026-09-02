import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { businessesApi } from "@/api/businesses";
import { FullPageSpinner } from "@/components/Spinner";
import { ChevronRightIcon } from "@/components/icons";

const inrFormatter = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 });

function StatCard({ label, value, sublabel, to }: { label: string; value: string; sublabel?: string; to: string }) {
  return (
    <Link
      to={to}
      className="card-surface focus-ring group flex flex-col gap-1 p-6 transition-transform hover:-translate-y-0.5"
    >
      <div className="flex items-center justify-between gap-2">
        <span className="text-xs font-medium uppercase tracking-wide text-ink-400">{label}</span>
        <ChevronRightIcon className="h-4 w-4 text-ink-300 transition-transform group-hover:translate-x-0.5 group-hover:text-signal-500" />
      </div>
      <span className="font-display text-3xl font-semibold text-ink-900">{value}</span>
      {sublabel && <span className="text-xs text-ink-400">{sublabel}</span>}
    </Link>
  );
}

export function BusinessDashboardPage() {
  const { data, isLoading } = useQuery({ queryKey: ["business", "dashboard"], queryFn: businessesApi.dashboard });

  if (isLoading || !data) return <FullPageSpinner />;

  const approved = data.approvalStatus === "APPROVED";
  const actionable = data.contentAwaitingReview + data.payoutsPayableCount;

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">Dashboard</h1>
        <p className="mt-1.5 text-ink-500">Your campaigns, content, and payouts at a glance.</p>
      </div>

      <div className="hero-card flex items-center gap-5">
        <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-signal-500 to-signal-800 text-3xl shadow-[0_18px_34px_-12px_rgba(0,0,0,0.55)]">
          {!approved ? "⏳" : actionable === 0 ? "✓" : "⏱"}
        </div>
        <div className="flex flex-col gap-1.5">
          <span
            className={
              "w-fit rounded-full px-2.5 py-0.5 font-mono text-[11px] font-semibold uppercase tracking-wide " +
              (!approved ? "bg-gold-soft text-gold-deep" : "bg-mint-soft text-mint-deep")
            }
          >
            {!approved ? "Awaiting approval" : actionable === 0 ? "All caught up" : "Needs review"}
          </span>
          <h2 className="font-display text-xl font-bold leading-tight text-ink-900">
            {!approved
              ? "Your account is awaiting admin approval"
              : `${data.liveCampaigns} live campaigns${actionable === 0 ? " — nothing waiting on you" : ""}`}
          </h2>
          <p className="text-sm text-ink-500">
            {!approved
              ? "You can't create campaigns until an admin approves your account. Check back soon."
              : `${data.contentAwaitingReview} content awaiting your review · ${inrFormatter.format(data.payoutsPayableAmountInr)} payable across ${data.payoutsPayableCount} creator${data.payoutsPayableCount === 1 ? "" : "s"}`}
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
        <StatCard label="Live campaigns" value={String(data.liveCampaigns)} to="/business/campaigns" />
        <StatCard
          label="Awaiting your review"
          value={String(data.contentAwaitingReview)}
          to="/business/content?status=SUBMITTED"
        />
        <StatCard
          label="Changes requested"
          value={String(data.contentChangesRequested)}
          sublabel="Waiting on the creator"
          to="/business/content?status=CHANGES_REQUESTED"
        />
        <StatCard
          label="Published content"
          value={String(data.publishedContentCount)}
          to="/business/content?status=PUBLISHED"
        />
        <StatCard
          label="Payouts ready to pay"
          value={inrFormatter.format(data.payoutsPayableAmountInr)}
          sublabel={`Across ${data.payoutsPayableCount} creator${data.payoutsPayableCount === 1 ? "" : "s"}`}
          to="/business/payouts?status=PAYABLE"
        />
      </div>

      <div className="now-bar">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-signal-500 to-signal-800 text-lg">
          {actionable === 0 ? "✓" : "⏱"}
        </div>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-bold text-ink-900">
            {actionable === 0 ? "Nothing waiting on you" : `${actionable} items waiting on you`}
          </p>
          <p className="text-xs text-ink-500">
            {data.contentAwaitingReview} to review · {data.payoutsPayableCount} payouts ready · updated just now
          </p>
        </div>
      </div>
    </div>
  );
}
