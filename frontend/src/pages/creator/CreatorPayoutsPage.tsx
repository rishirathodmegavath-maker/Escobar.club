import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { payoutsApi } from "@/api/payouts";
import { useAuth } from "@/auth/AuthContext";
import { StatusPill } from "@/components/StatusPill";
import { FullPageSpinner } from "@/components/Spinner";
import { EmptyState } from "@/components/EmptyState";
import { Pagination } from "@/components/Pagination";
import { Tabs } from "@/components/Tabs";
import { CoinIcon } from "@/components/icons";
import { formatCompactInr, formatCompactNumber } from "@/utils/formatIndianNumber";
import type { PayoutStatus } from "@/types";

const tabs: { label: string; value: PayoutStatus | undefined }[] = [
  { label: "Payable", value: "PAYABLE" },
  { label: "Pending KYC", value: "PENDING_KYC" },
  { label: "Paid", value: "PAID" },
  { label: "All", value: undefined },
];

function parseStatusParam(raw: string | null): PayoutStatus | undefined {
  if (raw === null) return "PAYABLE";
  if (raw === "ALL") return undefined;
  return raw as PayoutStatus;
}

export function CreatorPayoutsPage() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const status = parseStatusParam(searchParams.get("status"));
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["creator", "payouts", user?.id, status, page],
    queryFn: () => payoutsApi.listForCreator(user!.id, status, page),
    enabled: !!user,
  });

  const handleStatusChange = (next: PayoutStatus | undefined) => {
    setPage(0);
    setSearchParams(next ? { status: next } : { status: "ALL" }, { replace: true });
  };

  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">Earnings & Payouts</h1>
        <p className="mt-1.5 text-ink-500">What you've earned from published content, and what's already been paid.</p>
      </div>

      <Tabs tabs={tabs} value={status} onChange={handleStatusChange} />

      {isLoading ? (
        <FullPageSpinner />
      ) : !data || data.content.length === 0 ? (
        <EmptyState icon={<CoinIcon className="h-10 w-10" />} title="Nothing here yet" />
      ) : (
        <div className="card-surface overflow-x-auto p-0">
          <table className="w-full min-w-[640px] text-left text-sm">
            <thead>
              <tr className="border-b border-ink-100 text-xs uppercase tracking-wide text-ink-400">
                <th className="px-5 py-3 font-medium">Campaign</th>
                <th className="px-5 py-3 text-right font-medium tabular-nums">Views used</th>
                <th className="px-5 py-3 text-right font-medium tabular-nums">Amount</th>
                <th className="px-5 py-3 font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((payout) => (
                <tr key={payout.id} className="border-b border-ink-100 last:border-0 text-ink-700">
                  <td className="px-5 py-3.5 font-medium text-ink-900">{payout.campaignTitle}</td>
                  <td className="px-5 py-3.5 text-right font-mono tabular-nums">{formatCompactNumber(payout.viewCountUsed)}</td>
                  <td className="px-5 py-3.5 text-right font-mono tabular-nums">{formatCompactInr(payout.amountInr)}</td>
                  <td className="px-5 py-3.5">
                    <StatusPill status={payout.status} />
                    {payout.status === "PAID" && payout.paidAt && (
                      <p className="mt-1 text-xs text-ink-400">Paid {new Date(payout.paidAt).toLocaleDateString()}</p>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data && <Pagination page={data.page} totalPages={data.totalPages} last={data.last} onPageChange={setPage} />}
    </div>
  );
}
