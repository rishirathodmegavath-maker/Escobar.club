import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { businessesApi } from "@/api/businesses";
import { campaignsApi } from "@/api/campaigns";
import { FullPageSpinner } from "@/components/Spinner";
import { StatCard } from "@/components/StatCard";
import { StatusPill } from "@/components/StatusPill";
import { ProgressBar } from "@/components/ProgressBar";
import { ChevronRightIcon } from "@/components/icons";
import type { ContentStatus } from "@/types";

const inrFormatter = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 });
const numberFormatter = new Intl.NumberFormat("en-IN");
const dateFormatter = new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short" });

const CONTENT_STATUS_TILES: { label: string; status: ContentStatus }[] = [
  { label: "Submitted", status: "SUBMITTED" },
  { label: "Changes requested", status: "CHANGES_REQUESTED" },
  { label: "Link review", status: "PENDING_LINK_REVIEW" },
  { label: "Approved", status: "APPROVED" },
  { label: "Published", status: "PUBLISHED" },
  { label: "Rejected", status: "REJECTED" },
];

function CampaignAnalyticsSection({ campaigns }: { campaigns: { campaignId: number; title: string }[] }) {
  const [selectedId, setSelectedId] = useState<number | "">(campaigns[0]?.campaignId ?? "");
  const { data, isFetching } = useQuery({
    queryKey: ["campaign", "analytics", selectedId],
    queryFn: () => campaignsApi.analytics(selectedId as number),
    enabled: selectedId !== "",
  });

  if (campaigns.length === 0) return null;

  return (
    <div className="card-surface flex flex-col gap-4 p-6">
      <div className="flex items-center justify-between gap-3">
        <h2 className="font-display text-lg font-semibold text-ink-900">Campaign analytics</h2>
        <select
          value={selectedId}
          onChange={(e) => setSelectedId(Number(e.target.value))}
          className="focus-ring rounded-[10px] border border-ink-200 bg-surface-input px-3 py-2 text-sm text-ink-900"
        >
          {campaigns.map((c) => (
            <option key={c.campaignId} value={c.campaignId}>
              {c.title}
            </option>
          ))}
        </select>
      </div>
      {isFetching || !data ? (
        <p className="text-sm text-ink-400">Loading…</p>
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <div className="flex flex-col gap-0.5">
            <span className="text-xs uppercase tracking-wide text-ink-400">Views</span>
            <span className="font-display text-xl font-semibold text-ink-900">{numberFormatter.format(data.views)}</span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className="text-xs uppercase tracking-wide text-ink-400">Engagement</span>
            <span className="font-display text-xl font-semibold text-ink-900">{data.engagementRate}%</span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className="text-xs uppercase tracking-wide text-ink-400">Creators</span>
            <span className="font-display text-xl font-semibold text-ink-900">{data.creatorsCount}</span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className="text-xs uppercase tracking-wide text-ink-400">Committed</span>
            <span className="font-display text-xl font-semibold text-ink-900">{inrFormatter.format(data.budgetCommittedInr)}</span>
          </div>
        </div>
      )}
    </div>
  );
}

export function BusinessDashboardPage() {
  const { data, isLoading } = useQuery({ queryKey: ["business", "dashboard"], queryFn: businessesApi.dashboard });

  if (isLoading || !data) return <FullPageSpinner />;

  const approved = data.approvalStatus === "APPROVED";
  const actionable = data.contentAwaitingReview + data.payoutsPayableCount;
  const contentStatusCounts: Record<ContentStatus, number> = {
    DRAFT: 0,
    SUBMITTED: data.contentAwaitingReview,
    CHANGES_REQUESTED: data.contentChangesRequested,
    PENDING_LINK_REVIEW: data.contentPendingLinkReview,
    APPROVED: data.approvedContentCount,
    PUBLISHED: data.publishedContentCount,
    REJECTED: data.rejectedContentCount,
  };

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
        <StatCard
          label="Participating creators"
          value={String(data.participatingCreatorsCount)}
          to="/business/campaigns"
        />
        {data.campaignsNearBudgetCap > 0 && (
          <StatCard
            label="Near budget cap"
            value={String(data.campaignsNearBudgetCap)}
            sublabel="80%+ of the spend cap committed"
            to="/business/campaigns"
          />
        )}
      </div>

      {data.needsAttention.length > 0 && (
        <div className="flex flex-col gap-2">
          <h2 className="font-display text-lg font-semibold text-ink-900">Needs attention</h2>
          <div className="flex flex-col gap-2">
            {data.needsAttention.map((item, i) => (
              <Link
                key={i}
                to={item.actionPath}
                className="card-surface focus-ring group flex items-center justify-between gap-3 px-5 py-3.5 transition-transform hover:-translate-y-0.5"
              >
                <p className="text-sm text-ink-700">{item.message}</p>
                <span className="flex shrink-0 items-center gap-1 text-sm font-semibold text-signal-700">
                  {item.actionLabel}
                  <ChevronRightIcon className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
                </span>
              </Link>
            ))}
          </div>
        </div>
      )}

      {data.campaignsPreview.length > 0 && (
        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <h2 className="font-display text-lg font-semibold text-ink-900">My campaigns</h2>
            <Link to="/business/campaigns" className="text-sm font-semibold text-signal-700 hover:text-signal-800">
              View all
            </Link>
          </div>
          <div className="card-surface overflow-x-auto p-0">
            <table className="w-full min-w-[760px] text-left text-sm">
              <thead>
                <tr className="border-b border-ink-100 text-xs uppercase tracking-wide text-ink-400">
                  <th className="px-5 py-3 font-medium">Campaign</th>
                  <th className="px-5 py-3 font-medium">Status</th>
                  <th className="px-5 py-3 text-right font-medium tabular-nums">Creators</th>
                  <th className="px-5 py-3 text-right font-medium tabular-nums">Published</th>
                  <th className="px-5 py-3 text-right font-medium tabular-nums">Views</th>
                  <th className="px-5 py-3 text-right font-medium tabular-nums">Budget</th>
                </tr>
              </thead>
              <tbody>
                {data.campaignsPreview.map((c) => (
                  <tr key={c.campaignId} className="border-b border-ink-100 last:border-0 text-ink-700">
                    <td className="px-5 py-3.5 font-medium text-ink-900">
                      {c.title}
                      <p className="text-xs font-normal text-ink-400">Deadline {dateFormatter.format(new Date(c.submissionDeadline))}</p>
                    </td>
                    <td className="px-5 py-3.5">
                      <StatusPill status={c.status as "UPCOMING" | "LIVE" | "COMPLETED" | "DRAFT" | "CANCELLED"} />
                    </td>
                    <td className="px-5 py-3.5 text-right font-mono tabular-nums">{c.creatorsCount}</td>
                    <td className="px-5 py-3.5 text-right font-mono tabular-nums">{c.contentPublishedCount}</td>
                    <td className="px-5 py-3.5 text-right font-mono tabular-nums">{numberFormatter.format(c.views)}</td>
                    <td className="px-5 py-3.5 text-right font-mono tabular-nums">
                      {c.maxBudgetInr ? `${inrFormatter.format(c.committedBudgetInr)} / ${inrFormatter.format(c.maxBudgetInr)}` : "Unlimited"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {data.creatorActivity.length > 0 && (
        <div className="card-surface flex flex-col gap-3 p-6">
          <h2 className="font-display text-lg font-semibold text-ink-900">Creator activity</h2>
          <div className="flex flex-col gap-3">
            {data.creatorActivity.map((item, i) => (
              <div key={i} className="flex items-start gap-2.5 text-sm">
                <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-signal-500" />
                <p className="text-ink-600">{item.message}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="flex flex-col gap-3">
        <h2 className="font-display text-lg font-semibold text-ink-900">Content & reviews</h2>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
          {CONTENT_STATUS_TILES.map((tile) => (
            <Link
              key={tile.status}
              to={`/business/content?status=${tile.status}`}
              className="card-surface focus-ring flex flex-col gap-1 p-4 transition-transform hover:-translate-y-0.5"
            >
              <span className="text-xs uppercase tracking-wide text-ink-400">{tile.label}</span>
              <span className="font-display text-xl font-semibold text-ink-900">{contentStatusCounts[tile.status]}</span>
            </Link>
          ))}
        </div>
      </div>

      <div className="card-surface flex flex-col gap-4 p-6">
        <h2 className="font-display text-lg font-semibold text-ink-900">Budget & spending</h2>
        <ProgressBar
          percent={data.totalBudgetInr > 0 ? (data.totalCommittedInr / data.totalBudgetInr) * 100 : 0}
          tone={data.totalRemainingInr === 0 && data.totalBudgetInr > 0 ? "danger" : "gold"}
        />
        <div className="grid grid-cols-3 gap-4">
          <div className="flex flex-col gap-0.5">
            <span className="text-xs uppercase tracking-wide text-ink-400">Total budget</span>
            <span className="font-display text-lg font-semibold text-ink-900">{inrFormatter.format(data.totalBudgetInr)}</span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className="text-xs uppercase tracking-wide text-ink-400">Committed</span>
            <span className="font-display text-lg font-semibold text-ink-900">{inrFormatter.format(data.totalCommittedInr)}</span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className="text-xs uppercase tracking-wide text-ink-400">Remaining</span>
            <span className="font-display text-lg font-semibold text-ink-900">{inrFormatter.format(data.totalRemainingInr)}</span>
          </div>
        </div>
        <p className="text-xs text-ink-400">Only campaigns with a spend cap set are included in this total.</p>
      </div>

      <CampaignAnalyticsSection campaigns={data.campaignsPreview.map((c) => ({ campaignId: c.campaignId, title: c.title }))} />

      {data.topContent.length > 0 && (
        <div className="flex flex-col gap-3">
          <h2 className="font-display text-lg font-semibold text-ink-900">Top performing content</h2>
          <div className="flex flex-col gap-2">
            {data.topContent.map((item) => (
              <div key={item.contentId} className="card-surface flex items-center justify-between gap-4 px-5 py-3.5">
                <div className="min-w-0">
                  <p className="truncate font-medium text-ink-900">
                    {item.creatorDisplayName} · {item.campaignTitle}
                  </p>
                  <p className="text-xs text-ink-400">
                    {numberFormatter.format(item.views)} views · {numberFormatter.format(item.likes)} likes ·{" "}
                    {item.engagementRate}% engagement
                  </p>
                </div>
                {item.postUrl && (
                  <a
                    href={item.postUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="shrink-0 text-sm font-semibold text-signal-700 hover:text-signal-800"
                  >
                    View post
                  </a>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

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
