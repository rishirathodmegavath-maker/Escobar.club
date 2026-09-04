import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { adminApi } from "@/api/admin";
import { extractErrorMessage } from "@/api/client";
import { useToast } from "@/components/Toast";
import { Button } from "@/components/Button";
import { Input } from "@/components/Field";
import { StatusPill } from "@/components/StatusPill";
import { EmptyState } from "@/components/EmptyState";
import { CoinIcon } from "@/components/icons";
import { formatCompactInr } from "@/utils/formatIndianNumber";
import type { WalletTransaction } from "@/types";

function invalidateWalletQueries(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.invalidateQueries({ queryKey: ["admin", "wallets"] });
  queryClient.invalidateQueries({ queryKey: ["admin", "wallet-transactions"] });
  queryClient.invalidateQueries({ queryKey: ["admin", "dashboard"] });
}

function TransactionActions({ transaction }: { transaction: WalletTransaction }) {
  const [reversing, setReversing] = useState(false);
  const [note, setNote] = useState("");
  const queryClient = useQueryClient();
  const { push } = useToast();

  const reviewMutation = useMutation({
    mutationFn: (decision: "CONFIRMED" | "REJECTED") => adminApi.wallets.review(transaction.id, decision),
    onSuccess: (_, decision) => {
      push(decision === "CONFIRMED" ? "Top-up confirmed" : "Top-up rejected", "success");
      invalidateWalletQueries(queryClient);
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  const reverseMutation = useMutation({
    mutationFn: () => adminApi.wallets.reverse(transaction.id, note),
    onSuccess: () => {
      push("Transaction reversed", "success");
      setReversing(false);
      setNote("");
      invalidateWalletQueries(queryClient);
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  if (transaction.status === "PENDING") {
    return (
      <div className="flex justify-end gap-2">
        <Button size="sm" isLoading={reviewMutation.isPending} onClick={() => reviewMutation.mutate("CONFIRMED")}>
          Confirm
        </Button>
        <Button size="sm" variant="danger" isLoading={reviewMutation.isPending} onClick={() => reviewMutation.mutate("REJECTED")}>
          Reject
        </Button>
      </div>
    );
  }

  if (transaction.status === "CONFIRMED") {
    if (reversing) {
      return (
        <div className="flex items-center justify-end gap-2">
          <Input
            placeholder="Reason for reversal"
            value={note}
            onChange={(e) => setNote(e.target.value)}
            className="w-40"
          />
          <Button
            size="sm"
            variant="danger"
            isLoading={reverseMutation.isPending}
            disabled={!note.trim()}
            onClick={() => reverseMutation.mutate()}
          >
            Confirm
          </Button>
          <Button size="sm" variant="ghost" onClick={() => setReversing(false)}>
            Cancel
          </Button>
        </div>
      );
    }
    return (
      <div className="flex justify-end">
        <Button size="sm" variant="danger" onClick={() => setReversing(true)}>
          Reverse
        </Button>
      </div>
    );
  }

  return null;
}

export function AdminWalletTransactionTable({
  transactions,
  showBusinessColumn = true,
}: {
  transactions: WalletTransaction[];
  showBusinessColumn?: boolean;
}) {
  if (transactions.length === 0) {
    return <EmptyState icon={<CoinIcon className="h-10 w-10" />} title="No transactions" />;
  }

  return (
    <div className="card-surface overflow-x-auto p-0">
      <table className="w-full min-w-[860px] text-left text-sm">
        <thead>
          <tr className="border-b border-ink-100 text-xs uppercase tracking-wide text-ink-400">
            {showBusinessColumn && <th className="px-5 py-3 font-medium">Business</th>}
            <th className="px-5 py-3 font-medium">Type</th>
            <th className="px-5 py-3 font-medium">Source</th>
            <th className="px-5 py-3 text-right font-medium tabular-nums">Amount</th>
            <th className="px-5 py-3 font-medium">Status</th>
            <th className="px-5 py-3 font-medium">Date</th>
            <th className="px-5 py-3 text-right font-medium">Action</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((t) => (
            <tr key={t.id} className="border-b border-ink-100 last:border-0 text-ink-700">
              {showBusinessColumn && <td className="px-5 py-3.5 font-medium text-ink-900">{t.businessName ?? "—"}</td>}
              <td className="px-5 py-3.5 font-mono text-xs uppercase tracking-wide text-ink-400">{t.type}</td>
              <td className="px-5 py-3.5 text-xs text-ink-500">{t.fundingSource.replace(/_/g, " ")}</td>
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
              <td className="px-5 py-3.5 text-xs text-ink-400">{new Date(t.createdAt).toLocaleString()}</td>
              <td className="px-5 py-3.5">
                <TransactionActions transaction={t} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
