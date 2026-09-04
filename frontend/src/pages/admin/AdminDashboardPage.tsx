import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { adminApi } from "@/api/admin";
import { FullPageSpinner } from "@/components/Spinner";
import { StatusPill } from "@/components/StatusPill";
import { ChevronRightIcon, CoinIcon } from "@/components/icons";
import { formatCompactInr } from "@/utils/formatIndianNumber";

function StatCard({ label, value, to }: { label: string; value: number | string; to: string }) {
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

          <div>
            <h2 className="font-display text-lg font-semibold text-ink-900">Financial overview</h2>
            <p className="mt-1 text-sm text-ink-500">Escobar is the middleman — every businessman funds their own isolated wallet.</p>
          </div>

          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
            <StatCard label="Total funds held" value={formatCompactInr(data.totalFundsHeldInr, 0)} to="/admin/wallets" />
            <StatCard label="Total paid" value={formatCompactInr(data.totalPaidInr, 0)} to="/admin/wallets?tab=activity" />
            <StatCard label="Total available" value={formatCompactInr(data.totalAvailableInr, 0)} to="/admin/wallets" />
            <StatCard label="Businessman wallets" value={data.activeWalletsCount} to="/admin/wallets" />
            <StatCard
              label="Pending top-ups"
              value={data.pendingTopUpsCount}
              to="/admin/wallets?tab=activity&status=PENDING"
            />
          </div>

          {data.recentWalletActivity.length > 0 && (
            <div className="card-surface flex flex-col gap-4 p-6">
              <h3 className="font-display text-base font-semibold text-ink-900">Recent wallet activity</h3>
              <div className="flex flex-col divide-y divide-ink-100">
                {data.recentWalletActivity.map((t) => (
                  <div key={t.id} className="flex items-center justify-between gap-4 py-3 first:pt-0 last:pb-0">
                    <div className="flex items-center gap-3">
                      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-surface-hover text-ink-400">
                        <CoinIcon className="h-4 w-4" />
                      </div>
                      <div>
                        <p className="text-sm font-medium text-ink-900">{t.businessName ?? `Business #${t.businessId}`}</p>
                        <p className="text-xs text-ink-400">
                          {t.type === "CREDIT" ? "Money added" : "Money paid out"} · {new Date(t.createdAt).toLocaleString()}
                        </p>
                      </div>
                    </div>
                    <div className="flex flex-col items-end gap-1">
                      <span className={`font-mono text-sm tabular-nums ${t.type === "CREDIT" ? "text-mint-deep" : "text-ink-700"}`}>
                        {t.type === "CREDIT" ? "+" : "−"}
                        {formatCompactInr(t.amountInr, 0)}
                      </span>
                      <StatusPill status={t.status} />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

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
