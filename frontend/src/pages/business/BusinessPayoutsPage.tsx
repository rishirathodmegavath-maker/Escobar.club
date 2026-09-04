import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { payoutsApi } from "@/api/payouts";
import { extractErrorMessage } from "@/api/client";
import { useAuth } from "@/auth/AuthContext";
import { useToast } from "@/components/Toast";
import { Button } from "@/components/Button";
import { Input } from "@/components/Field";
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

function MarkPaidAction({ contentId }: { contentId: number }) {
  const [marking, setMarking] = useState(false);
  const [note, setNote] = useState("");
  const queryClient = useQueryClient();
  const { push } = useToast();

  const mutation = useMutation({
    mutationFn: () => payoutsApi.markPaid(contentId, note || undefined),
    onSuccess: () => {
      push("Marked as paid", "success");
      queryClient.invalidateQueries({ queryKey: ["business", "payouts"] });
      setMarking(false);
      setNote("");
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  if (marking) {
    return (
      <div className="flex items-center justify-end gap-2">
        <Input
          placeholder="Reference (optional)"
          value={note}
          onChange={(e) => setNote(e.target.value)}
          className="w-40"
        />
        <Button size="sm" isLoading={mutation.isPending} onClick={() => mutation.mutate()}>
          Confirm
        </Button>
        <Button size="sm" variant="ghost" onClick={() => setMarking(false)}>
          Cancel
        </Button>
      </div>
    );
  }

  return (
    <div className="flex justify-end">
      <Button size="sm" onClick={() => setMarking(true)}>
        Mark as paid
      </Button>
    </div>
  );
}

export function BusinessPayoutsPage() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const status = parseStatusParam(searchParams.get("status"));
  const [page, setPage] = useState(0);
  const [exporting, setExporting] = useState(false);
  const { push } = useToast();

  const { data, isLoading } = useQuery({
    queryKey: ["business", "payouts", user?.id, status, page],
    queryFn: () => payoutsApi.listForBusiness(user!.id, status, page),
    enabled: !!user,
  });

  const handleStatusChange = (next: PayoutStatus | undefined) => {
    setPage(0);
    setSearchParams(next ? { status: next } : { status: "ALL" }, { replace: true });
  };

  const handleExport = async () => {
    if (!user) return;
    setExporting(true);
    try {
      const blob = await payoutsApi.exportCsv(user.id, status);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "payouts.csv";
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
    <div className="mx-auto flex max-w-4xl flex-col gap-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl font-semibold text-ink-900">Payouts</h1>
          <p className="mt-1.5 text-ink-500">What you owe creators for published content, and what's already been paid.</p>
        </div>
        <Button variant="secondary" size="sm" isLoading={exporting} onClick={handleExport}>
          Export CSV
        </Button>
      </div>

      <Tabs tabs={tabs} value={status} onChange={handleStatusChange} />

      {isLoading ? (
        <FullPageSpinner />
      ) : !data || data.content.length === 0 ? (
        <EmptyState icon={<CoinIcon className="h-10 w-10" />} title="Nothing here" />
      ) : (
        <div className="card-surface overflow-x-auto p-0">
          <table className="w-full min-w-[760px] text-left text-sm">
            <thead>
              <tr className="border-b border-ink-100 text-xs uppercase tracking-wide text-ink-400">
                <th className="px-5 py-3 font-medium">Creator</th>
                <th className="px-5 py-3 font-medium">Campaign</th>
                <th className="px-5 py-3 text-right font-medium tabular-nums">Views used</th>
                <th className="px-5 py-3 text-right font-medium tabular-nums">Amount</th>
                <th className="px-5 py-3 font-medium">Status</th>
                <th className="px-5 py-3 text-right font-medium">Action</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((payout) => (
                <tr key={payout.id} className="border-b border-ink-100 last:border-0 text-ink-700">
                  <td className="px-5 py-3.5 font-medium text-ink-900">{payout.creatorDisplayName ?? "—"}</td>
                  <td className="px-5 py-3.5">{payout.campaignTitle}</td>
                  <td className="px-5 py-3.5 text-right font-mono tabular-nums">{formatCompactNumber(payout.viewCountUsed)}</td>
                  <td className="px-5 py-3.5 text-right font-mono tabular-nums">{formatCompactInr(payout.amountInr)}</td>
                  <td className="px-5 py-3.5">
                    <StatusPill status={payout.status} />
                    {payout.status === "PAID" && payout.paidAt && (
                      <p className="mt-1 text-xs text-ink-400">Paid {new Date(payout.paidAt).toLocaleDateString()}</p>
                    )}
                  </td>
                  <td className="px-5 py-3.5">
                    {payout.status === "PAYABLE" && <MarkPaidAction contentId={payout.contentId} />}
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
