import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { contentApi } from "@/api/content";
import { useAuth } from "@/auth/AuthContext";
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

  const { data, isLoading } = useQuery({
    queryKey: ["content", "review-queue", user?.id, status, page],
    queryFn: () => contentApi.reviewQueue(user!.id, status, page),
    enabled: !!user,
  });

  const handleStatusChange = (next: ContentStatus | undefined) => {
    setPage(0);
    setSearchParams(next ? { status: next } : { status: "ALL" }, { replace: true });
  };

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">Content review queue</h1>
        <p className="mt-1.5 text-ink-500">Approve, reject, or request changes on submitted creator content.</p>
      </div>

      <Tabs tabs={tabs} value={status} onChange={handleStatusChange} />

      {isLoading ? (
        <FullPageSpinner />
      ) : !data || data.content.length === 0 ? (
        <EmptyState icon={<ImageStackIcon className="h-10 w-10" />} title="Nothing here" description="Check back once creators submit content." />
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 xl:grid-cols-5">
          {data.content.map((item) => (
            <ContentReviewCard key={item.id} content={item} />
          ))}
        </div>
      )}

      {data && <Pagination page={data.page} totalPages={data.totalPages} last={data.last} onPageChange={setPage} />}
    </div>
  );
}
