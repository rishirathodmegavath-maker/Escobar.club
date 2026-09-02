import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ContentRecord } from "@/types";
import { payoutsApi } from "@/api/payouts";
import { extractErrorMessage } from "@/api/client";
import { useAuth } from "@/auth/AuthContext";
import { Button } from "@/components/Button";
import { Input } from "@/components/Field";
import { StatusPill } from "@/components/StatusPill";
import { useToast } from "@/components/Toast";

const inrFormatter = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 });

export function PayoutPanel({ content }: { content: ContentRecord }) {
  const { user } = useAuth();
  const [markingPaid, setMarkingPaid] = useState(false);
  const [note, setNote] = useState("");
  const queryClient = useQueryClient();
  const { push } = useToast();

  const { data, isLoading } = useQuery({
    queryKey: ["payout", content.id],
    queryFn: () => payoutsApi.getForContent(content.id),
    retry: false,
  });

  const markPaidMutation = useMutation({
    mutationFn: () => payoutsApi.markPaid(content.id, note || undefined),
    onSuccess: () => {
      push("Marked as paid", "success");
      queryClient.invalidateQueries({ queryKey: ["payout", content.id] });
      setMarkingPaid(false);
      setNote("");
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  if (isLoading) return null;
  if (!data) {
    return (
      <div className="border-t border-ink-100 pt-4">
        <h4 className="text-xs font-semibold uppercase tracking-wide text-ink-400">Payout</h4>
        <p className="mt-1 text-sm text-ink-400">Sync metrics to calculate payout eligibility.</p>
      </div>
    );
  }

  return (
    <div className="border-t border-ink-100 pt-4">
      <div className="flex items-center justify-between gap-3">
        <h4 className="text-xs font-semibold uppercase tracking-wide text-ink-400">Payout</h4>
        <StatusPill status={data.status} />
      </div>

      <div className="mt-3 flex items-center justify-between rounded-lg bg-paper-100 px-3 py-2.5">
        <div>
          <p className="font-mono text-lg font-semibold text-ink-900">
            {inrFormatter.format(data.amountInr)} ({data.viewCountUsed.toLocaleString()})
          </p>
          <p className="font-mono text-xs text-ink-400">
            ({inrFormatter.format(data.rateUsed)} / 1,000 views)
          </p>
        </div>
        {data.status === "PAID" && data.paidAt && (
          <p className="font-mono text-xs text-ink-400">Paid {new Date(data.paidAt).toLocaleDateString()}</p>
        )}
      </div>

      {user?.role === "BUSINESS" && data.status === "PAYABLE" && (
        markingPaid ? (
          <div className="mt-3 flex flex-col gap-2">
            <Input
              placeholder="Payment reference (optional)"
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
            <div className="flex gap-2">
              <Button size="sm" isLoading={markPaidMutation.isPending} onClick={() => markPaidMutation.mutate()}>
                Confirm paid
              </Button>
              <Button size="sm" variant="ghost" onClick={() => setMarkingPaid(false)}>
                Cancel
              </Button>
            </div>
          </div>
        ) : (
          <Button size="sm" className="mt-3" onClick={() => setMarkingPaid(true)}>
            Mark as paid
          </Button>
        )
      )}
      {data.status === "PENDING_KYC" && (
        <p className="mt-2 text-xs text-ink-400">Awaiting creator KYC verification before this can be paid.</p>
      )}
    </div>
  );
}
