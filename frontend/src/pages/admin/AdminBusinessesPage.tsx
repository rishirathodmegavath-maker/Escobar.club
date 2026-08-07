import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { adminApi } from "@/api/admin";
import { extractErrorMessage } from "@/api/client";
import { useToast } from "@/components/Toast";
import { Button } from "@/components/Button";
import { TextArea } from "@/components/Field";
import { StatusPill } from "@/components/StatusPill";
import { FullPageSpinner } from "@/components/Spinner";
import { EmptyState } from "@/components/EmptyState";
import { Pagination } from "@/components/Pagination";
import { Tabs } from "@/components/Tabs";
import { MegaphoneIcon } from "@/components/icons";
import type { AdminBusinessSummary, ApprovalStatus } from "@/types";

const tabs: { label: string; value: ApprovalStatus | undefined }[] = [
  { label: "Pending", value: "PENDING" },
  { label: "Approved", value: "APPROVED" },
  { label: "Rejected", value: "REJECTED" },
  { label: "All", value: undefined },
];

function parseStatusParam(raw: string | null): ApprovalStatus | undefined {
  if (raw === null) return "PENDING";
  if (raw === "ALL") return undefined;
  return raw as ApprovalStatus;
}

function BusinessCard({ business }: { business: AdminBusinessSummary }) {
  const [rejecting, setRejecting] = useState(false);
  const [note, setNote] = useState("");
  const queryClient = useQueryClient();
  const { push } = useToast();

  const mutation = useMutation({
    mutationFn: (status: "APPROVED" | "REJECTED") => adminApi.reviewBusiness(business.userId, { status, note: note || undefined }),
    onSuccess: (_, status) => {
      push(status === "APPROVED" ? "Business approved" : "Business rejected", "success");
      queryClient.invalidateQueries({ queryKey: ["admin", "businesses"] });
      setRejecting(false);
      setNote("");
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  return (
    <div className="card-surface flex flex-col gap-4 p-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h3 className="font-display text-lg font-semibold text-ink-900">{business.companyName}</h3>
          <p className="text-xs text-ink-400">{business.email}</p>
        </div>
        <StatusPill status={business.approvalStatus} />
      </div>

      <dl className="grid grid-cols-2 gap-x-4 gap-y-1 text-sm text-ink-600">
        <dt className="text-ink-400">GST number</dt>
        <dd className="font-mono">{business.gstNumber}</dd>
        <dt className="text-ink-400">Contact person</dt>
        <dd>{business.contactPersonName}</dd>
      </dl>

      {!rejecting && business.approvalStatus !== "APPROVED" && (
        <div className="flex flex-wrap gap-2 border-t border-ink-100 pt-4">
          <Button size="sm" isLoading={mutation.isPending} onClick={() => mutation.mutate("APPROVED")}>
            Approve
          </Button>
          <Button size="sm" variant="danger" onClick={() => setRejecting(true)}>
            Reject
          </Button>
        </div>
      )}
      {!rejecting && business.approvalStatus === "APPROVED" && (
        <div className="flex flex-wrap gap-2 border-t border-ink-100 pt-4">
          <Button size="sm" variant="danger" onClick={() => setRejecting(true)}>
            Revoke approval
          </Button>
        </div>
      )}

      {rejecting && (
        <div className="flex flex-col gap-3 border-t border-ink-100 pt-4">
          <TextArea label="Reason (optional)" value={note} onChange={(e) => setNote(e.target.value)} rows={2} />
          <div className="flex gap-2">
            <Button size="sm" variant="danger" isLoading={mutation.isPending} onClick={() => mutation.mutate("REJECTED")}>
              Confirm reject
            </Button>
            <Button size="sm" variant="ghost" onClick={() => setRejecting(false)}>
              Cancel
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

export function AdminBusinessesPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const status = parseStatusParam(searchParams.get("status"));
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "businesses", status, page],
    queryFn: () => adminApi.listBusinesses({ status, page }),
  });

  const handleStatusChange = (next: ApprovalStatus | undefined) => {
    setPage(0);
    setSearchParams(next ? { status: next } : { status: "ALL" }, { replace: true });
  };

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">Brands</h1>
        <p className="mt-1.5 text-ink-500">Approve or reject business accounts before they can publish campaigns.</p>
      </div>

      <Tabs tabs={tabs} value={status} onChange={handleStatusChange} />

      {isLoading ? (
        <FullPageSpinner />
      ) : !data || data.content.length === 0 ? (
        <EmptyState icon={<MegaphoneIcon className="h-10 w-10" />} title="Nothing here" />
      ) : (
        <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
          {data.content.map((business) => (
            <BusinessCard key={business.userId} business={business} />
          ))}
        </div>
      )}

      {data && <Pagination page={data.page} totalPages={data.totalPages} last={data.last} onPageChange={setPage} />}
    </div>
  );
}
