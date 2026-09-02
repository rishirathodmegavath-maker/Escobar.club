import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ContentRecord } from "@/types";
import { Button } from "@/components/Button";
import { Spinner } from "@/components/Spinner";
import { metricsApi } from "@/api/metrics";
import { extractErrorMessage } from "@/api/client";
import { useToast } from "@/components/Toast";

const compactNumber = new Intl.NumberFormat("en-US", { notation: "compact", maximumFractionDigits: 1 });

function formatCount(value: number | null): string {
  return value === null ? "—" : compactNumber.format(value);
}

function timeAgo(iso: string): string {
  const seconds = Math.max(0, Math.floor((Date.now() - new Date(iso).getTime()) / 1000));
  if (seconds < 60) return "just now";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

export function ContentMetricsPanel({ content }: { content: ContentRecord }) {
  const [historyOpen, setHistoryOpen] = useState(false);
  const queryClient = useQueryClient();
  const { push } = useToast();

  const historyQuery = useQuery({
    queryKey: ["content-metrics", "history", content.id],
    queryFn: () => metricsApi.history(content.id),
  });

  const syncMutation = useMutation({
    mutationFn: () => metricsApi.sync(content.id),
    onSuccess: () => {
      push("Metrics synced", "success");
      queryClient.invalidateQueries({ queryKey: ["content-metrics", "history", content.id] });
    },
    onError: (err) => push(extractErrorMessage(err, "Could not sync metrics"), "error"),
  });

  const latest = historyQuery.data?.content[0] ?? null;

  return (
    <div className="border-t border-ink-100 pt-4">
      <div className="flex items-center justify-between gap-4">
        <h4 className="text-xs font-semibold uppercase tracking-wide text-ink-400">Performance</h4>
        <Button size="sm" variant="secondary" isLoading={syncMutation.isPending} onClick={() => syncMutation.mutate()}>
          Sync now
        </Button>
      </div>

      {historyQuery.isLoading ? (
        <Spinner className="mt-3" />
      ) : (
        <>
          <div className="mt-3 grid grid-cols-2 gap-3">
            <div className="rounded-lg bg-gold-soft px-3 py-2.5">
              <p className="text-[11px] uppercase tracking-wide text-gold-deep">Views</p>
              <p className="font-mono text-lg font-semibold text-ink-900">{formatCount(latest?.viewCount ?? null)}</p>
            </div>
            <div className="rounded-lg bg-gold-soft px-3 py-2.5">
              <p className="text-[11px] uppercase tracking-wide text-gold-deep">Total views</p>
              <p className="font-mono text-lg font-semibold text-ink-900">
                {latest?.viewCount == null ? "—" : latest.viewCount.toLocaleString()}
              </p>
            </div>
          </div>

          <p className="mt-2 text-xs text-ink-400">
            {latest ? `Last synced ${timeAgo(latest.fetchedAt)}` : "Not yet synced"}
          </p>

          {historyQuery.data && historyQuery.data.content.length > 1 && (
            <div className="mt-2">
              <button
                type="button"
                onClick={() => setHistoryOpen((open) => !open)}
                className="focus-ring text-xs font-medium text-signal-600 hover:text-signal-700"
              >
                {historyOpen ? "Hide history" : "View history"}
              </button>
              {historyOpen && (
                <div className="mt-2 overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="text-ink-400">
                        <th className="pb-1 pr-4 font-medium">Synced</th>
                        <th className="pb-1 pr-4 font-medium tabular-nums">Likes</th>
                        <th className="pb-1 pr-4 font-medium tabular-nums">Comments</th>
                        <th className="pb-1 font-medium tabular-nums">Views</th>
                      </tr>
                    </thead>
                    <tbody>
                      {historyQuery.data.content.map((snapshot) => (
                        <tr key={snapshot.id} className="border-t border-ink-100 text-ink-700">
                          <td className="py-1.5 pr-4 font-mono">{timeAgo(snapshot.fetchedAt)}</td>
                          <td className="py-1.5 pr-4 font-mono tabular-nums">{formatCount(snapshot.likeCount)}</td>
                          <td className="py-1.5 pr-4 font-mono tabular-nums">{formatCount(snapshot.commentCount)}</td>
                          <td className="py-1.5 font-mono tabular-nums">{formatCount(snapshot.viewCount)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
}
