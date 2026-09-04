import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { businessWalletApi } from "@/api/wallet";
import { useAuth } from "@/auth/AuthContext";
import { Button } from "@/components/Button";
import { StatusPill } from "@/components/StatusPill";
import { FullPageSpinner } from "@/components/Spinner";
import { EmptyState } from "@/components/EmptyState";
import { Pagination } from "@/components/Pagination";
import { CoinIcon } from "@/components/icons";
import { AddMoneyModal } from "@/components/AddMoneyModal";
import { formatCompactInr } from "@/utils/formatIndianNumber";
import type { WalletTransaction } from "@/types";

function descriptionFor(t: WalletTransaction): string {
  if (t.fundingSource === "CAMPAIGN_PAYMENT") return t.campaignTitle ? `Payout for ${t.campaignTitle}` : "Campaign payout";
  if (t.fundingSource === "REVERSAL") return "Correction";
  if (t.fundingSource === "ADMIN_MANUAL") return "Credited by admin";
  return t.note || "Money added";
}

export function BusinessWalletPage() {
  const { user } = useAuth();
  const [page, setPage] = useState(0);
  const [addingMoney, setAddingMoney] = useState(false);

  const { data: summary, isLoading: summaryLoading } = useQuery({
    queryKey: ["business", "wallet", "summary", user?.id],
    queryFn: () => businessWalletApi.getSummary(user!.id),
    enabled: !!user,
  });

  const { data: transactions, isLoading: transactionsLoading } = useQuery({
    queryKey: ["business", "wallet", "transactions", user?.id, page],
    queryFn: () => businessWalletApi.listTransactions(user!.id, { page }),
    enabled: !!user,
  });

  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">Wallet</h1>
        <p className="mt-1.5 text-ink-500">
          Record funds you've sent to Escobar. This does not initiate a real payment — an admin confirms each top-up before it
          becomes available to spend.
        </p>
      </div>

      {summaryLoading || !summary ? (
        <FullPageSpinner />
      ) : (
        <>
          <div className="hero-card flex flex-wrap items-center justify-between gap-6">
            <div className="flex flex-col gap-1.5">
              <span className="text-xs font-medium uppercase tracking-wide text-ink-400">Available balance</span>
              <span className="font-display text-4xl font-bold text-ink-900">{formatCompactInr(summary.availableBalanceInr)}</span>
            </div>
            <Button onClick={() => setAddingMoney(true)}>+ Add Money</Button>
          </div>

          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
            <div className="card-surface flex flex-col gap-1 p-6">
              <span className="text-xs font-medium uppercase tracking-wide text-ink-400">Total added</span>
              <span className="font-display text-2xl font-semibold text-ink-900">{formatCompactInr(summary.totalAddedInr)}</span>
            </div>
            <div className="card-surface flex flex-col gap-1 p-6">
              <span className="text-xs font-medium uppercase tracking-wide text-ink-400">Total paid</span>
              <span className="font-display text-2xl font-semibold text-ink-900">{formatCompactInr(summary.totalPaidInr)}</span>
            </div>
          </div>
        </>
      )}

      <h2 className="font-display text-lg font-semibold text-ink-900">Transaction history</h2>

      {transactionsLoading ? (
        <FullPageSpinner />
      ) : !transactions || transactions.content.length === 0 ? (
        <EmptyState
          icon={<CoinIcon className="h-10 w-10" />}
          title="No transactions yet"
          description="Add money to get started."
        />
      ) : (
        <div className="card-surface overflow-x-auto p-0">
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead>
              <tr className="border-b border-ink-100 text-xs uppercase tracking-wide text-ink-400">
                <th className="px-5 py-3 font-medium">Date</th>
                <th className="px-5 py-3 font-medium">Description</th>
                <th className="px-5 py-3 text-right font-medium tabular-nums">Amount</th>
                <th className="px-5 py-3 font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {transactions.content.map((t) => (
                <tr key={t.id} className="border-b border-ink-100 last:border-0 text-ink-700">
                  <td className="px-5 py-3.5">{new Date(t.createdAt).toLocaleDateString()}</td>
                  <td className="px-5 py-3.5 font-medium text-ink-900">{descriptionFor(t)}</td>
                  <td
                    className={`px-5 py-3.5 text-right font-mono tabular-nums ${
                      t.type === "CREDIT" ? "text-mint-deep" : "text-ink-700"
                    }`}
                  >
                    {t.type === "CREDIT" ? "+" : "−"}
                    {formatCompactInr(t.amountInr)}
                  </td>
                  <td className="px-5 py-3.5">
                    <StatusPill status={t.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {transactions && (
        <Pagination page={transactions.page} totalPages={transactions.totalPages} last={transactions.last} onPageChange={setPage} />
      )}

      {addingMoney && user && <AddMoneyModal businessId={user.id} onClose={() => setAddingMoney(false)} />}
    </div>
  );
}
