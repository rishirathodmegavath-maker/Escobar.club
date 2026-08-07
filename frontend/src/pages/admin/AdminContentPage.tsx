import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { adminApi } from "@/api/admin";
import { StatusPill } from "@/components/StatusPill";
import { FullPageSpinner } from "@/components/Spinner";
import { EmptyState } from "@/components/EmptyState";
import { Pagination } from "@/components/Pagination";
import { Tabs } from "@/components/Tabs";
import { EyeIcon } from "@/components/icons";
import type { ContentStatus } from "@/types";

const tabs: { label: string; value: ContentStatus | undefined }[] = [
  { label: "Published", value: "PUBLISHED" },
  { label: "Submitted", value: "SUBMITTED" },
  { label: "All", value: undefined },
];

// Published content is where views/likes/comments actually exist (metrics only accrue after a
// creator publishes and syncs), so that's the useful default rather than "everything".
function parseStatusParam(raw: string | null): ContentStatus | undefined {
  if (raw === null) return "PUBLISHED";
  if (raw === "ALL") return undefined;
  return raw as ContentStatus;
}

const compactNumber = new Intl.NumberFormat("en-US", { notation: "compact", maximumFractionDigits: 1 });

// The backend omits null fields from its JSON entirely (no metrics synced yet), so these arrive
// as `undefined`, not `null` - guard against both.
function formatCount(value: number | null | undefined): string {
  return value == null ? "—" : compactNumber.format(value);
}

function timeAgo(iso: string | null | undefined): string {
  if (!iso) return "Never synced";
  const seconds = Math.max(0, Math.floor((Date.now() - new Date(iso).getTime()) / 1000));
  if (seconds < 60) return "just now";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

export function AdminContentPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const status = parseStatusParam(searchParams.get("status"));
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "content", status, page],
    queryFn: () => adminApi.listContent({ status, page }),
  });

  const handleStatusChange = (next: ContentStatus | undefined) => {
    setPage(0);
    setSearchParams(next ? { status: next } : { status: "ALL" }, { replace: true });
  };

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">Content &amp; metrics</h1>
        <p className="mt-1.5 text-ink-500">
          Views, likes, and comments as last scraped from each creator's published post - the same figures
          creators and businesses see on their own dashboards.
        </p>
      </div>

      <Tabs tabs={tabs} value={status} onChange={handleStatusChange} />

      {isLoading ? (
        <FullPageSpinner />
      ) : !data || data.content.length === 0 ? (
        <EmptyState icon={<EyeIcon className="h-10 w-10" />} title="Nothing here" />
      ) : (
        <div className="card-surface overflow-x-auto p-0">
          <table className="w-full min-w-[880px] text-left text-sm">
            <thead>
              <tr className="border-b border-ink-100 text-xs uppercase tracking-wide text-ink-400">
                <th className="px-5 py-3 font-medium">Creator</th>
                <th className="px-5 py-3 font-medium">Business</th>
                <th className="px-5 py-3 font-medium">Campaign</th>
                <th className="px-5 py-3 font-medium">Status</th>
                <th className="px-5 py-3 text-right font-medium tabular-nums">Views</th>
                <th className="px-5 py-3 text-right font-medium tabular-nums">Likes</th>
                <th className="px-5 py-3 text-right font-medium tabular-nums">Comments</th>
                <th className="px-5 py-3 font-medium">Last synced</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((item) => (
                <tr key={item.id} className="border-b border-ink-100 last:border-0 text-ink-700">
                  <td className="px-5 py-3.5">
                    <p className="font-medium text-ink-900">{item.creatorDisplayName ?? item.creatorEmail}</p>
                    <p className="text-xs text-ink-400">{item.creatorEmail}</p>
                  </td>
                  <td className="px-5 py-3.5">{item.businessCompanyName ?? "—"}</td>
                  <td className="px-5 py-3.5">
                    {item.postUrl ? (
                      <a
                        href={item.postUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="focus-ring text-signal-600 hover:text-signal-700 hover:underline"
                      >
                        {item.campaignTitle}
                      </a>
                    ) : (
                      item.campaignTitle
                    )}
                  </td>
                  <td className="px-5 py-3.5">
                    <StatusPill status={item.status} />
                  </td>
                  <td className="px-5 py-3.5 text-right font-mono tabular-nums">{formatCount(item.viewCount)}</td>
                  <td className="px-5 py-3.5 text-right font-mono tabular-nums">{formatCount(item.likeCount)}</td>
                  <td className="px-5 py-3.5 text-right font-mono tabular-nums">{formatCount(item.commentCount)}</td>
                  <td className="px-5 py-3.5 text-xs text-ink-400">{timeAgo(item.metricsLastSyncedAt)}</td>
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
