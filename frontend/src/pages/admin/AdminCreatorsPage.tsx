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
import { UserIcon } from "@/components/icons";
import type { AdminCreatorSummary, KycStatus } from "@/types";

const tabs: { label: string; value: KycStatus | undefined }[] = [
  { label: "Pending KYC", value: "PENDING" },
  { label: "Verified", value: "VERIFIED" },
  { label: "Rejected", value: "REJECTED" },
  { label: "All", value: undefined },
];

// No filter shows every creator by default (unlike Brands/Campaigns, this page's prior default -
// preserved so it doesn't silently start hiding already-verified creators from admins who land
// here via the sidebar). `?status=PENDING` etc. still deep-links from the dashboard's KYC stat.
function parseStatusParam(raw: string | null): KycStatus | undefined {
  if (raw === null || raw === "ALL") return undefined;
  return raw as KycStatus;
}

function CreatorCard({ creator }: { creator: AdminCreatorSummary }) {
  const [rejecting, setRejecting] = useState(false);
  const [note, setNote] = useState("");
  const queryClient = useQueryClient();
  const { push } = useToast();

  const kycMutation = useMutation({
    mutationFn: (status: "VERIFIED" | "REJECTED") => adminApi.reviewCreatorKyc(creator.userId, { status, reviewNote: note || undefined }),
    onSuccess: () => {
      push(`KYC ${creator.userId} reviewed`, "success");
      queryClient.invalidateQueries({ queryKey: ["admin", "creators"] });
      setRejecting(false);
      setNote("");
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  const statusMutation = useMutation({
    mutationFn: (active: boolean) => adminApi.setCreatorActive(creator.userId, active),
    onSuccess: (_, active) => {
      push(active ? "Creator activated" : "Creator suspended", "success");
      queryClient.invalidateQueries({ queryKey: ["admin", "creators"] });
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  return (
    <div className="card-surface flex flex-col gap-4 p-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h3 className="font-display text-lg font-semibold text-ink-900">{creator.displayName ?? creator.email}</h3>
          <p className="text-xs text-ink-400">{creator.email}</p>
        </div>
        <div className="flex shrink-0 flex-col items-end gap-1.5">
          {creator.kycStatus && <StatusPill status={creator.kycStatus} />}
          <StatusPill status={creator.active ? "APPROVED" : "REJECTED"} />
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2 border-t border-ink-100 pt-4">
        {creator.kycStatus === "PENDING" && !rejecting && (
          <>
            <Button size="sm" isLoading={kycMutation.isPending} onClick={() => kycMutation.mutate("VERIFIED")}>
              Approve KYC
            </Button>
            <Button size="sm" variant="danger" onClick={() => setRejecting(true)}>
              Reject KYC
            </Button>
          </>
        )}
        <Button size="sm" variant="secondary" isLoading={statusMutation.isPending} onClick={() => statusMutation.mutate(!creator.active)}>
          {creator.active ? "Suspend" : "Activate"}
        </Button>
      </div>

      {rejecting && (
        <div className="flex flex-col gap-3 border-t border-ink-100 pt-4">
          <TextArea label="Reason (optional)" value={note} onChange={(e) => setNote(e.target.value)} rows={2} />
          <div className="flex gap-2">
            <Button size="sm" variant="danger" isLoading={kycMutation.isPending} onClick={() => kycMutation.mutate("REJECTED")}>
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

export function AdminCreatorsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const status = parseStatusParam(searchParams.get("status"));
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "creators", status, page],
    queryFn: () => adminApi.listCreators({ status, page }),
  });

  const handleStatusChange = (next: KycStatus | undefined) => {
    setPage(0);
    setSearchParams(next ? { status: next } : { status: "ALL" }, { replace: true });
  };

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">Creators</h1>
        <p className="mt-1.5 text-ink-500">Review creator KYC submissions and manage account access.</p>
      </div>

      <Tabs tabs={tabs} value={status} onChange={handleStatusChange} />

      {isLoading ? (
        <FullPageSpinner />
      ) : !data || data.content.length === 0 ? (
        <EmptyState icon={<UserIcon className="h-10 w-10" />} title="No creators yet" />
      ) : (
        <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
          {data.content.map((creator) => (
            <CreatorCard key={creator.userId} creator={creator} />
          ))}
        </div>
      )}

      {data && <Pagination page={data.page} totalPages={data.totalPages} last={data.last} onPageChange={setPage} />}
    </div>
  );
}
