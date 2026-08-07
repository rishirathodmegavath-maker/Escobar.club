import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { adminApi } from "@/api/admin";
import { FullPageSpinner } from "@/components/Spinner";
import { ChevronRightIcon } from "@/components/icons";

function StatCard({ label, value, to }: { label: string; value: number; to: string }) {
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
    </Link>
  );
}

export function AdminDashboardPage() {
  const { data, isLoading } = useQuery({ queryKey: ["admin", "dashboard"], queryFn: adminApi.dashboard });
  const pending = data ? data.pendingCampaignApprovals + data.pendingCreatorKyc : 0;

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">Admin dashboard</h1>
        <p className="mt-1.5 text-ink-500">Platform-wide overview of brands, creators, and campaigns.</p>
      </div>

      {isLoading || !data ? (
        <FullPageSpinner />
      ) : (
        <>
          <div className="hero-card flex items-center gap-5">
            <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-signal-500 to-signal-800 text-3xl shadow-[0_18px_34px_-12px_rgba(0,0,0,0.55)]">
              {pending === 0 ? "✓" : "⏱"}
            </div>
            <div className="flex flex-col gap-1.5">
              <span className="w-fit rounded-full bg-mint-soft px-2.5 py-0.5 font-mono text-[11px] font-semibold uppercase tracking-wide text-mint-deep">
                {pending === 0 ? "All systems normal" : "Needs review"}
              </span>
              <h2 className="font-display text-xl font-bold leading-tight text-ink-900">
                {data.totalBrands} brands, {data.totalCreators} creators, {data.totalCampaigns} campaigns
                {pending === 0 ? " — nothing waiting on you" : ""}
              </h2>
              <p className="text-sm text-ink-500">
                {data.pendingCampaignApprovals} pending campaign approvals · {data.pendingCreatorKyc} pending KYC reviews
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
            <StatCard label="Total brands" value={data.totalBrands} to="/admin/businesses?status=ALL" />
            <StatCard label="Total creators" value={data.totalCreators} to="/admin/creators?status=ALL" />
            <StatCard label="Total campaigns" value={data.totalCampaigns} to="/admin/campaigns?status=ALL" />
            <StatCard
              label="Pending campaign approvals"
              value={data.pendingCampaignApprovals}
              to="/admin/campaigns?status=PENDING"
            />
            <StatCard label="Pending creator KYC" value={data.pendingCreatorKyc} to="/admin/creators?status=PENDING" />
          </div>

          <div className="now-bar">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-signal-500 to-signal-800 text-lg">
              {pending === 0 ? "✓" : "⏱"}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-bold text-ink-900">{pending === 0 ? "Platform healthy" : `${pending} items waiting on you`}</p>
              <p className="text-xs text-ink-500">{pending} pending approvals · updated just now</p>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
