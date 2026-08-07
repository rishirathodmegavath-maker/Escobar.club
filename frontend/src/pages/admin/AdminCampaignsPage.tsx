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
import { CompassIcon } from "@/components/icons";
import type { AdminCampaignSummary, ApprovalStatus, CampaignDisplayStatus } from "@/types";

const tabs: { label: string; value: ApprovalStatus | undefined }[] = [
  { label: "Pending", value: "PENDING" },
  { label: "Approved", value: "APPROVED" },
  { label: "Rejected", value: "REJECTED" },
  { label: "All", value: undefined },
];

const displayStatusOptions: CampaignDisplayStatus[] = ["UPCOMING", "LIVE", "HOT", "CLOSED"];

// No `status` param -> defaults to Pending (surfaces actionable items first, matches sidebar nav
// behavior); `?status=ALL` is the explicit sentinel for the "All" tab since `undefined` can't
// round-trip through a query string.
function parseStatusParam(raw: string | null): ApprovalStatus | undefined {
  if (raw === null) return "PENDING";
  if (raw === "ALL") return undefined;
  return raw as ApprovalStatus;
}

function CampaignCard({ campaign }: { campaign: AdminCampaignSummary }) {
  const [rejecting, setRejecting] = useState(false);
  const [note, setNote] = useState("");
  const [displayStatus, setDisplayStatus] = useState<CampaignDisplayStatus>(campaign.adminDisplayStatus ?? "UPCOMING");
  const queryClient = useQueryClient();
  const { push } = useToast();

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["admin", "campaigns"] });

  const approvalMutation = useMutation({
    mutationFn: (status: "APPROVED" | "REJECTED") => adminApi.reviewCampaign(campaign.id, { status, note: note || undefined }),
    onSuccess: (_, status) => {
      push(status === "APPROVED" ? "Campaign approved" : "Campaign rejected", "success");
      invalidate();
      setRejecting(false);
      setNote("");
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  const displayStatusMutation = useMutation({
    mutationFn: (status: CampaignDisplayStatus) => adminApi.setCampaignDisplayStatus(campaign.id, status),
    onSuccess: () => {
      push("Display status updated", "success");
      invalidate();
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  return (
    <div className="card-surface flex flex-col gap-4 p-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h3 className="font-display text-lg font-semibold text-ink-900">{campaign.title}</h3>
          <p className="text-xs text-ink-400">{campaign.businessCompanyName ?? "Unknown business"}</p>
        </div>
        <div className="flex shrink-0 flex-col items-end gap-1.5">
          <StatusPill status={campaign.approvalStatus} />
          {campaign.adminDisplayStatus && <StatusPill status={campaign.adminDisplayStatus} />}
        </div>
      </div>

      <p className="font-mono text-xs text-ink-400">
        Live {campaign.publishStartAt} – {campaign.publishEndAt}
      </p>

      {!rejecting && (
        <div className="flex flex-wrap items-center gap-2 border-t border-ink-100 pt-4">
          {campaign.approvalStatus !== "APPROVED" && (
            <Button size="sm" isLoading={approvalMutation.isPending} onClick={() => approvalMutation.mutate("APPROVED")}>
              Approve
            </Button>
          )}
          <Button size="sm" variant="danger" onClick={() => setRejecting(true)}>
            {campaign.approvalStatus === "APPROVED" ? "Revoke approval" : "Reject"}
          </Button>
        </div>
      )}

      {rejecting && (
        <div className="flex flex-col gap-3 border-t border-ink-100 pt-4">
          <TextArea label="Reason (optional)" value={note} onChange={(e) => setNote(e.target.value)} rows={2} />
          <div className="flex gap-2">
            <Button size="sm" variant="danger" isLoading={approvalMutation.isPending} onClick={() => approvalMutation.mutate("REJECTED")}>
              Confirm reject
            </Button>
            <Button size="sm" variant="ghost" onClick={() => setRejecting(false)}>
              Cancel
            </Button>
          </div>
        </div>
      )}

      <div className="flex flex-wrap items-end gap-2 border-t border-ink-100 pt-4">
        <label className="flex flex-col gap-1.5">
          <span className="text-sm font-medium text-ink-700">Display status shown to creators</span>
          <select
            className="focus-ring w-full rounded-lg border border-ink-200 bg-surface-input px-3.5 py-2.5 text-sm text-ink-900"
            value={displayStatus}
            onChange={(e) => setDisplayStatus(e.target.value as CampaignDisplayStatus)}
          >
            {displayStatusOptions.map((option) => (
              <option key={option} value={option}>
                {option[0] + option.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </label>
        <Button size="sm" variant="secondary" isLoading={displayStatusMutation.isPending} onClick={() => displayStatusMutation.mutate(displayStatus)}>
          Set
        </Button>
      </div>
    </div>
  );
}

export function AdminCampaignsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const status = parseStatusParam(searchParams.get("status"));
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "campaigns", status, page],
    queryFn: () => adminApi.listCampaigns({ status, page }),
  });

  const handleStatusChange = (next: ApprovalStatus | undefined) => {
    setPage(0);
    setSearchParams(next ? { status: next } : { status: "ALL" }, { replace: true });
  };

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">Campaigns</h1>
        <p className="mt-1.5 text-ink-500">Only admin-approved campaigns are visible to creators on Discover.</p>
      </div>

      <Tabs tabs={tabs} value={status} onChange={handleStatusChange} />

      {isLoading ? (
        <FullPageSpinner />
      ) : !data || data.content.length === 0 ? (
        <EmptyState icon={<CompassIcon className="h-10 w-10" />} title="Nothing here" />
      ) : (
        <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
          {data.content.map((campaign) => (
            <CampaignCard key={campaign.id} campaign={campaign} />
          ))}
        </div>
      )}

      {data && <Pagination page={data.page} totalPages={data.totalPages} last={data.last} onPageChange={setPage} />}
    </div>
  );
}
