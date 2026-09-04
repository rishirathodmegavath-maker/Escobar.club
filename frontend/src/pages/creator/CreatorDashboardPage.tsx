import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { creatorsApi } from "@/api/creators";
import { FullPageSpinner } from "@/components/Spinner";
import { Avatar } from "@/components/Avatar";
import { StatCard } from "@/components/StatCard";
import { StatusPill } from "@/components/StatusPill";
import { ProgressBar } from "@/components/ProgressBar";
import { DiscoverCampaignCard } from "@/features/campaigns/DiscoverCampaignCard";
import { Button } from "@/components/Button";
import { ChevronRightIcon, SparkIcon } from "@/components/icons";

const inrFormatter = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 });
const numberFormatter = new Intl.NumberFormat("en-IN");

function greeting(): string {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 17) return "Good afternoon";
  return "Good evening";
}

export function CreatorDashboardPage() {
  const { data: profile } = useQuery({ queryKey: ["creator", "me"], queryFn: creatorsApi.getMine });
  const { data, isLoading } = useQuery({ queryKey: ["creator", "dashboard"], queryFn: creatorsApi.dashboard });

  if (isLoading || !data) return <FullPageSpinner />;

  return (
    <div className="flex flex-col gap-6">
      <div className="hero-card flex flex-col gap-5 sm:flex-row sm:items-center">
        <Avatar
          name={profile?.displayName ?? "Creator"}
          imageUrl={profile?.profilePictureUrl}
          size={64}
          className="shrink-0 text-xl shadow-[0_18px_34px_-12px_rgba(0,0,0,0.35)]"
        />
        <div className="flex min-w-0 flex-1 flex-col gap-1.5">
          <div className="flex flex-wrap items-center gap-2">
            {data.kycStatus ? (
              <StatusPill status={data.kycStatus} />
            ) : (
              <span className="w-fit rounded-full bg-gold-soft px-2.5 py-0.5 font-mono text-[11px] font-semibold uppercase tracking-wide text-gold-deep">
                KYC not started
              </span>
            )}
            <span className="text-xs text-ink-400">Profile {data.profileCompletionPercent}% complete</span>
          </div>
          <h1 className="font-display text-2xl font-bold leading-tight text-ink-900">
            {greeting()}{profile?.displayName ? `, ${profile.displayName}` : ""}
          </h1>
          <p className="text-sm text-ink-500">
            {data.activeCampaignsCount} active campaign{data.activeCampaignsCount === 1 ? "" : "s"} ·{" "}
            {data.submissionStatus.submitted + data.submissionStatus.changesRequested} submission
            {data.submissionStatus.submitted + data.submissionStatus.changesRequested === 1 ? "" : "s"} need attention
          </p>
        </div>
        {data.kycStatus !== "VERIFIED" && (
          <Link to="/creator/kyc" className="shrink-0">
            <Button size="sm">{data.kycStatus ? "Check KYC status" : "Complete KYC"}</Button>
          </Link>
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

      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Active campaigns" value={String(data.activeCampaignsCount)} to="/creator/content" />
        <StatCard
          label="Awaiting review"
          value={String(data.submissionStatus.submitted)}
          to="/creator/content"
        />
        <StatCard
          label="Payable earnings"
          value={inrFormatter.format(data.earnings.payableInr)}
          to="/creator/payouts"
        />
        <StatCard
          label="Paid this month"
          value={inrFormatter.format(data.earnings.thisMonthPaidInr)}
          to="/creator/payouts"
        />
      </div>

      {data.recommendedCampaigns.length > 0 && (
        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <h2 className="font-display text-lg font-semibold text-ink-900">Recommended campaigns</h2>
            <Link to="/" className="text-sm font-semibold text-signal-700 hover:text-signal-800">
              View all
            </Link>
          </div>
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {data.recommendedCampaigns.map((campaign) => (
              <DiscoverCampaignCard key={campaign.id} campaign={campaign} />
            ))}
          </div>
        </div>
      )}

      {data.activeCampaigns.length > 0 && (
        <div className="flex flex-col gap-3">
          <h2 className="font-display text-lg font-semibold text-ink-900">My active campaigns</h2>
          <div className="card-surface overflow-x-auto p-0">
            <table className="w-full min-w-[560px] text-left text-sm">
              <thead>
                <tr className="border-b border-ink-100 text-xs uppercase tracking-wide text-ink-400">
                  <th className="px-5 py-3 font-medium">Campaign</th>
                  <th className="px-5 py-3 font-medium">Status</th>
                  <th className="px-5 py-3 text-right font-medium tabular-nums">Views</th>
                  <th className="px-5 py-3 text-right font-medium tabular-nums">Earnings</th>
                </tr>
              </thead>
              <tbody>
                {data.activeCampaigns.map((c) => (
                  <tr key={c.campaignId} className="border-b border-ink-100 last:border-0 text-ink-700">
                    <td className="px-5 py-3.5 font-medium text-ink-900">{c.title}</td>
                    <td className="px-5 py-3.5">
                      <StatusPill status={c.status as "UPCOMING" | "LIVE"} />
                    </td>
                    <td className="px-5 py-3.5 text-right font-mono tabular-nums">{numberFormatter.format(c.views)}</td>
                    <td className="px-5 py-3.5 text-right font-mono tabular-nums">{inrFormatter.format(c.earningsInr)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <div className="flex flex-col gap-3">
        <h2 className="font-display text-lg font-semibold text-ink-900">Submission status</h2>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
          {(
            [
              ["Submitted", data.submissionStatus.submitted],
              ["Changes requested", data.submissionStatus.changesRequested],
              ["Approved", data.submissionStatus.approved],
              ["Link review", data.submissionStatus.pendingLinkReview],
              ["Published", data.submissionStatus.published],
              ["Rejected", data.submissionStatus.rejected],
            ] as const
          ).map(([label, count]) => (
            <Link
              key={label}
              to="/creator/content"
              className="card-surface focus-ring flex flex-col gap-1 p-4 transition-transform hover:-translate-y-0.5"
            >
              <span className="text-xs uppercase tracking-wide text-ink-400">{label}</span>
              <span className="font-display text-xl font-semibold text-ink-900">{count}</span>
            </Link>
          ))}
        </div>
      </div>

      {data.topContent.length > 0 && (
        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <h2 className="font-display text-lg font-semibold text-ink-900">Top performing content</h2>
            <Link to="/creator/content" className="text-sm font-semibold text-signal-700 hover:text-signal-800">
              View all
            </Link>
          </div>
          <div className="flex flex-col gap-2">
            {data.topContent.map((item) => (
              <div key={item.contentId} className="card-surface flex items-center justify-between gap-4 px-5 py-3.5">
                <div className="min-w-0">
                  <p className="truncate font-medium text-ink-900">{item.campaignTitle}</p>
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

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
        <div className="card-surface flex flex-col gap-4 p-6">
          <div className="flex items-center justify-between">
            <h2 className="font-display text-lg font-semibold text-ink-900">Earnings & payouts</h2>
            <Link to="/creator/payouts" className="text-sm font-semibold text-signal-700 hover:text-signal-800">
              View all
            </Link>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Stat label="Pending KYC" value={inrFormatter.format(data.earnings.pendingKycInr)} />
            <Stat label="Payable" value={inrFormatter.format(data.earnings.payableInr)} />
            <Stat label="Paid to date" value={inrFormatter.format(data.earnings.paidInr)} />
            <Stat label="Paid this month" value={inrFormatter.format(data.earnings.thisMonthPaidInr)} />
          </div>
          {data.recentPayouts.length > 0 && (
            <div className="flex flex-col gap-1.5 border-t border-ink-100 pt-3">
              {data.recentPayouts.slice(0, 4).map((p) => (
                <div key={p.contentId} className="flex items-center justify-between text-sm">
                  <span className="truncate text-ink-600">{p.campaignTitle}</span>
                  <span className="flex shrink-0 items-center gap-2">
                    <span className="font-mono tabular-nums text-ink-900">{inrFormatter.format(p.amountInr)}</span>
                    <StatusPill status={p.status} />
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="card-surface flex flex-col gap-4 p-6">
          <h2 className="font-display text-lg font-semibold text-ink-900">Recent activity</h2>
          {data.recentActivity.length === 0 ? (
            <p className="text-sm text-ink-400">Nothing yet — your activity will show up here.</p>
          ) : (
            <div className="flex flex-col gap-3">
              {data.recentActivity.map((item, i) => (
                <div key={i} className="flex items-start gap-2.5 text-sm">
                  <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-signal-500" />
                  <p className="text-ink-600">{item.message}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {data.profileCompletionPercent < 100 && (
        <div className="card-surface flex flex-col gap-4 p-6">
          <div className="flex items-center justify-between">
            <h2 className="font-display text-lg font-semibold text-ink-900">Complete your profile</h2>
            <span className="font-mono text-sm text-ink-400">{data.profileCompletionPercent}%</span>
          </div>
          <ProgressBar percent={data.profileCompletionPercent} tone="gold" />
          <p className="text-sm text-ink-500">Missing: {data.profileCompletionMissing.join(", ")}</p>
          <Link to="/creator/profile/edit" className="w-fit">
            <Button size="sm" variant="secondary">
              Complete profile
            </Button>
          </Link>
        </div>
      )}

      <div className="flex flex-wrap gap-3">
        <Link to="/">
          <Button variant="secondary" size="sm">
            <SparkIcon className="h-4 w-4" />
            Discover campaigns
          </Button>
        </Link>
        <Link to="/creator/content">
          <Button variant="secondary" size="sm">
            My submissions
          </Button>
        </Link>
        <Link to="/creator/kyc">
          <Button variant="secondary" size="sm">
            My KYC
          </Button>
        </Link>
      </div>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-0.5">
      <span className="text-xs uppercase tracking-wide text-ink-400">{label}</span>
      <span className="font-display text-lg font-semibold text-ink-900">{value}</span>
    </div>
  );
}
