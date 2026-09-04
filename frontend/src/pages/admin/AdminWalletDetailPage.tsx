import { useState } from "react";
import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { adminApi } from "@/api/admin";
import { FullPageSpinner } from "@/components/Spinner";
import { Pagination } from "@/components/Pagination";
import { Button } from "@/components/Button";
import { AdminCreditModal } from "@/components/AdminCreditModal";
import { AdminWalletTransactionTable } from "@/features/wallet/AdminWalletTransactionTable";

const inrFormatter = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 });

export function AdminWalletDetailPage() {
  const { businessId } = useParams<{ businessId: string }>();
  const id = Number(businessId);
  const [page, setPage] = useState(0);
  const [crediting, setCrediting] = useState(false);

  const { data: summary, isLoading: summaryLoading } = useQuery({
    queryKey: ["admin", "wallets", "detail", id],
    queryFn: () => adminApi.wallets.get(id),
    enabled: Number.isFinite(id),
  });

  const { data: transactions, isLoading: transactionsLoading } = useQuery({
    queryKey: ["admin", "wallet-transactions", { businessId: id }, page],
    queryFn: () => adminApi.wallets.listTransactions({ businessId: id, page }),
    enabled: Number.isFinite(id),
  });

  if (summaryLoading || !summary) return <FullPageSpinner />;

  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">{summary.businessName ?? `Business #${id}`}</h1>
        <p className="mt-1.5 text-ink-500">Wallet balance and full transaction ledger for this business.</p>
      </div>

      <div className="hero-card flex flex-wrap items-center justify-between gap-6">
        <div className="flex flex-col gap-1.5">
          <span className="text-xs font-medium uppercase tracking-wide text-ink-400">Current balance</span>
          <span className="font-display text-4xl font-bold text-ink-900">{inrFormatter.format(summary.availableBalanceInr)}</span>
        </div>
        <Button onClick={() => setCrediting(true)}>Credit wallet</Button>
      </div>

      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
        <div className="card-surface flex flex-col gap-1 p-6">
          <span className="text-xs font-medium uppercase tracking-wide text-ink-400">Total added</span>
          <span className="font-display text-2xl font-semibold text-ink-900">{inrFormatter.format(summary.totalAddedInr)}</span>
        </div>
        <div className="card-surface flex flex-col gap-1 p-6">
          <span className="text-xs font-medium uppercase tracking-wide text-ink-400">Total paid</span>
          <span className="font-display text-2xl font-semibold text-ink-900">{inrFormatter.format(summary.totalPaidInr)}</span>
        </div>
      </div>

      <h2 className="font-display text-lg font-semibold text-ink-900">Transaction history</h2>

      {transactionsLoading ? (
        <FullPageSpinner />
      ) : (
        <AdminWalletTransactionTable transactions={transactions?.content ?? []} showBusinessColumn={false} />
      )}

      {transactions && (
        <Pagination page={transactions.page} totalPages={transactions.totalPages} last={transactions.last} onPageChange={setPage} />
      )}

      {crediting && <AdminCreditModal businessId={id} onClose={() => setCrediting(false)} />}
    </div>
  );
}
