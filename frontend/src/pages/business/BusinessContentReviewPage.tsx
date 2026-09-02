import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { contentApi } from "@/api/content";
import { useAuth } from "@/auth/AuthContext";
import { extractErrorMessage } from "@/api/client";
import { useToast } from "@/components/Toast";
import { Button } from "@/components/Button";
import { FullPageSpinner } from "@/components/Spinner";
import { EmptyState } from "@/components/EmptyState";
import { ImageStackIcon } from "@/components/icons";
import { Tabs } from "@/components/Tabs";
import { Pagination } from "@/components/Pagination";
import { ContentReviewCard } from "@/features/content/ContentReviewCard";
import type { ContentStatus } from "@/types";

const tabs: { label: string; value: ContentStatus | undefined }[] = [
  { label: "Submitted", value: "SUBMITTED" },
  { label: "Changes requested", value: "CHANGES_REQUESTED" },
  { label: "Approved", value: "APPROVED" },
  { label: "Published", value: "PUBLISHED" },
  { label: "Rejected", value: "REJECTED" },
  { label: "All", value: undefined },
];

function parseStatusParam(raw: string | null): ContentStatus | undefined {
  if (raw === null) return "SUBMITTED";
  if (raw === "ALL") return undefined;
  return raw as ContentStatus;
}

export function BusinessContentReviewPage() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const status = parseStatusParam(searchParams.get("status"));
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const queryClient = useQueryClient();
  const { push } = useToast();

  const { data, isLoading } = useQuery({
    queryKey: ["content", "review-queue", user?.id, status, page],
    queryFn: () => contentApi.reviewQueue(user!.id, status, page),
    enabled: !!user,
  });

  // Bulk-select only makes sense within one tab/page's set of ids - stale selections from a
  // previous view would silently apply a decision to the wrong content otherwise.
  useEffect(() => {
    setSelected(new Set());
  }, [status, page]);

  const handleStatusChange = (next: ContentStatus | undefined) => {
    setPage(0);
    setSearchParams(next ? { status: next } : { status: "ALL" }, { replace: true });
  };

  const toggleSelect = (id: number) => {
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const bulkMutation = useMutation({
    mutationFn: (decision: "APPROVED" | "REJECTED") => contentApi.reviewBulk(Array.from(selected), { decision }),
    onSuccess: (result) => {
      if (result.failures.length === 0) {
        push(`${result.succeeded} item${result.succeeded === 1 ? "" : "s"} updated.`, "success");
      } else {
        push(
          `${result.succeeded} updated, ${result.failures.length} failed (${result.failures[0].reason}${result.failures.length > 1 ? ", …" : ""}).`,
          result.succeeded > 0 ? "success" : "error",
        );
      }
      queryClient.invalidateQueries({ queryKey: ["content", "review-queue"] });
      setSelected(new Set());
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  const canBulkReview = status === "SUBMITTED";

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">Content review queue</h1>
        <p className="mt-1.5 text-ink-500">Approve, reject, or request changes on submitted creator content.</p>
      </div>

      <Tabs tabs={tabs} value={status} onChange={handleStatusChange} />

      {canBulkReview && selected.size > 0 && (
        <div className="card-surface flex items-center justify-between gap-3 p-3.5">
          <span className="text-sm font-medium text-ink-700">{selected.size} selected</span>
          <div className="flex items-center gap-2">
            <Button size="sm" isLoading={bulkMutation.isPending} onClick={() => bulkMutation.mutate("APPROVED")}>
              Approve all
            </Button>
            <Button size="sm" variant="danger" isLoading={bulkMutation.isPending} onClick={() => bulkMutation.mutate("REJECTED")}>
              Reject all
            </Button>
            <Button size="sm" variant="ghost" onClick={() => setSelected(new Set())}>
              Clear
            </Button>
          </div>
        </div>
      )}

      {isLoading ? (
        <FullPageSpinner />
      ) : !data || data.content.length === 0 ? (
        <EmptyState icon={<ImageStackIcon className="h-10 w-10" />} title="Nothing here" description="Check back once creators submit content." />
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 xl:grid-cols-5">
          {data.content.map((item) => (
            <ContentReviewCard
              key={item.id}
              content={item}
              selectable={canBulkReview}
              selected={selected.has(item.id)}
              onToggleSelect={() => toggleSelect(item.id)}
            />
          ))}
        </div>
      )}

      {data && <Pagination page={data.page} totalPages={data.totalPages} last={data.last} onPageChange={setPage} />}
    </div>
  );
}
