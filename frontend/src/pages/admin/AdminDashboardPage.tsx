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

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">Admin dashboard</h1>
        <p className="mt-1.5 text-ink-500">Platform-wide overview of brands, creators, and campaigns.</p>
      </div>

      {isLoading || !data ? (
        <FullPageSpinner />
      ) : (
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
      )}
    </div>
  );
}
