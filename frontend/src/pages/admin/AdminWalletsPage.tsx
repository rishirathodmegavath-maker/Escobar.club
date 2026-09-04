import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link, useSearchParams } from "react-router-dom";
import { adminApi } from "@/api/admin";
import { extractErrorMessage } from "@/api/client";
import { useToast } from "@/components/Toast";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { FullPageSpinner } from "@/components/Spinner";
import { EmptyState } from "@/components/EmptyState";
import { Pagination } from "@/components/Pagination";
import { Tabs } from "@/components/Tabs";
import { Button } from "@/components/Button";
import { CoinIcon } from "@/components/icons";
import { AdminWalletTransactionTable } from "@/features/wallet/AdminWalletTransactionTable";
import { formatCompactInr } from "@/utils/formatIndianNumber";
import type { WalletTransactionStatus, WalletTransactionType } from "@/types";

const searchInputClasses =
  "focus-ring w-full max-w-sm rounded-[10px] border border-ink-200 bg-surface-input px-4 py-2.5 text-sm text-ink-900 placeholder:text-ink-300 sm:max-w-xs";
const selectClasses =
  "focus-ring rounded-lg border border-ink-200 bg-surface-input px-3.5 py-2.5 text-sm text-ink-900";

function WalletsTab() {
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebouncedValue(search, 300);

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "wallets", debouncedSearch, page],
    queryFn: () => adminApi.wallets.list({ search: debouncedSearch || undefined, page }),
  });

  return (
    <div className="flex flex-col gap-5">
      <input
        value={search}
        onChange={(e) => {
          setSearch(e.target.value);
          setPage(0);
        }}
        placeholder="Search by business name…"
        className={searchInputClasses}
      />

      {isLoading ? (
        <FullPageSpinner />
      ) : !data || data.content.length === 0 ? (
        <EmptyState icon={<CoinIcon className="h-10 w-10" />} title="No wallets found" />
      ) : (
        <div className="card-surface overflow-x-auto p-0">
          <table className="w-full min-w-[760px] text-left text-sm">
            <thead>
              <tr className="border-b border-ink-100 text-xs uppercase tracking-wide text-ink-400">
                <th className="px-5 py-3 font-medium">Businessman</th>
                <th className="px-5 py-3 text-right font-medium tabular-nums">Available</th>
                <th className="px-5 py-3 text-right font-medium tabular-nums">Total added</th>
                <th className="px-5 py-3 text-right font-medium tabular-nums">Total paid</th>
                <th className="px-5 py-3 font-medium">Last activity</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((wallet) => (
                <tr key={wallet.businessId} className="border-b border-ink-100 last:border-0 text-ink-700">
                  <td className="px-5 py-3.5">
                    <Link
                      to={`/admin/wallets/${wallet.businessId}`}
                      className="focus-ring font-medium text-ink-900 hover:text-signal-700"
                    >
                      {wallet.businessName ?? `Business #${wallet.businessId}`}
                    </Link>
                  </td>
                  <td className="px-5 py-3.5 text-right font-mono tabular-nums">{formatCompactInr(wallet.availableBalanceInr)}</td>
                  <td className="px-5 py-3.5 text-right font-mono tabular-nums">{formatCompactInr(wallet.totalAddedInr)}</td>
                  <td className="px-5 py-3.5 text-right font-mono tabular-nums">{formatCompactInr(wallet.totalPaidInr)}</td>
                  <td className="px-5 py-3.5 text-xs text-ink-400">
                    {wallet.lastActivityAt ? new Date(wallet.lastActivityAt).toLocaleString() : "—"}
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

function ActivityTab({ initialStatus }: { initialStatus: WalletTransactionStatus | "" }) {
  const [search, setSearch] = useState("");
  const [type, setType] = useState<WalletTransactionType | "">("");
  const [status, setStatus] = useState<WalletTransactionStatus | "">(initialStatus);
  const [page, setPage] = useState(0);
  const [exporting, setExporting] = useState(false);
  const debouncedSearch = useDebouncedValue(search, 300);
  const { push } = useToast();

  const filters = {
    search: debouncedSearch || undefined,
    type: type || undefined,
    status: status || undefined,
    page,
  };

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "wallet-transactions", filters],
    queryFn: () => adminApi.wallets.listTransactions(filters),
  });

  const handleExport = async () => {
    setExporting(true);
    try {
      const blob = await adminApi.wallets.exportCsv({ search: filters.search, type: filters.type, status: filters.status });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "wallet-transactions.csv";
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      push(extractErrorMessage(err), "error");
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-3">
          <input
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            placeholder="Search by business name…"
            className={searchInputClasses}
          />
          <select
            value={type}
            onChange={(e) => {
              setType(e.target.value as WalletTransactionType | "");
              setPage(0);
            }}
            className={selectClasses}
          >
            <option value="">All types</option>
            <option value="CREDIT">Credit</option>
            <option value="DEBIT">Debit</option>
          </select>
          <select
            value={status}
            onChange={(e) => {
              setStatus(e.target.value as WalletTransactionStatus | "");
              setPage(0);
            }}
            className={selectClasses}
          >
            <option value="">All statuses</option>
            <option value="PENDING">Pending</option>
            <option value="CONFIRMED">Confirmed</option>
            <option value="REJECTED">Rejected</option>
            <option value="REVERSED">Reversed</option>
          </select>
        </div>
        <Button variant="secondary" size="sm" isLoading={exporting} onClick={handleExport}>
          Export CSV
        </Button>
      </div>

      {isLoading ? <FullPageSpinner /> : <AdminWalletTransactionTable transactions={data?.content ?? []} />}

      {data && <Pagination page={data.page} totalPages={data.totalPages} last={data.last} onPageChange={setPage} />}
    </div>
  );
}

export function AdminWalletsPage() {
  const [searchParams] = useSearchParams();
  const [tab, setTab] = useState<"wallets" | "activity">(searchParams.get("tab") === "activity" ? "activity" : "wallets");
  const initialStatus = (searchParams.get("status") as WalletTransactionStatus | null) ?? "";

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">Wallets</h1>
        <p className="mt-1.5 text-ink-500">Every businessman's wallet balance and the full ledger of money movements.</p>
      </div>

      <Tabs
        tabs={[
          { label: "All wallets", value: "wallets" },
          { label: "Activity", value: "activity" },
        ]}
        value={tab}
        onChange={setTab}
      />

      {tab === "wallets" ? <WalletsTab /> : <ActivityTab initialStatus={initialStatus} />}
    </div>
  );
}
