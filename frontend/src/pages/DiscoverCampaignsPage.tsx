import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { campaignsApi } from "@/api/campaigns";
import { CampaignCard } from "@/features/campaigns/CampaignCard";
import { EmptyState } from "@/components/EmptyState";
import { FullPageSpinner } from "@/components/Spinner";
import { CompassIcon } from "@/components/icons";
import { Pagination } from "@/components/Pagination";
import { Tabs } from "@/components/Tabs";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import type { CampaignCategory } from "@/types";

const CATEGORY_TABS: { label: string; value: CampaignCategory }[] = [
  { label: "🔥 Hot", value: "HOT" },
  { label: "⏳ Upcoming", value: "UPCOMING" },
  { label: "🟢 Live", value: "LIVE" },
  { label: "✅ Completed", value: "COMPLETED" },
];

const CATEGORY_DESCRIPTIONS: Record<CampaignCategory, string> = {
  HOT: "High-priority campaigns that need creators immediately.",
  UPCOMING: "Accepting creator submissions now, ahead of their publishing window.",
  LIVE: "Currently publishing — submissions are closed, views are being counted.",
  COMPLETED: "Publishing has ended for these campaigns.",
};

const CATEGORY_EMPTY_TEXT: Record<CampaignCategory, string> = {
  HOT: "No high-priority campaigns right now — check the Upcoming tab for everything open for submissions.",
  UPCOMING: "No campaigns are open for submissions right now. Check back soon.",
  LIVE: "No campaigns are currently live.",
  COMPLETED: "No campaigns have completed their publishing window yet.",
};

export function DiscoverCampaignsPage() {
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState<CampaignCategory>("HOT");
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebouncedValue(search, 300);

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ["campaigns", category, debouncedSearch, page],
    queryFn: () => campaignsApi.searchPublic({ search: debouncedSearch || undefined, category, page }),
  });

  return (
    <div className="flex flex-col gap-8">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">Discover campaigns</h1>
        <p className="mt-1.5 max-w-2xl text-ink-500">{CATEGORY_DESCRIPTIONS[category]}</p>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <Tabs
          tabs={CATEGORY_TABS}
          value={category}
          onChange={(value) => {
            setCategory(value);
            setPage(0);
          }}
        />
        <input
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(0);
          }}
          placeholder="Search by campaign title…"
          className="focus-ring w-full max-w-sm rounded-[10px] border border-ink-200 bg-surface-input px-4 py-2.5 text-sm text-ink-900 placeholder:text-ink-300 sm:max-w-xs"
        />
      </div>

      {isLoading ? (
        <FullPageSpinner />
      ) : !data || data.content.length === 0 ? (
        <EmptyState
          icon={<CompassIcon className="h-10 w-10" />}
          title="No campaigns found"
          description={search ? "Try a different search term." : CATEGORY_EMPTY_TEXT[category]}
        />
      ) : (
        <>
          {category === "HOT" && !search && (
            <div className="hero-card flex items-center gap-6">
              <div className="flex h-[92px] w-[92px] shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-signal-500 to-signal-800 text-4xl shadow-[0_18px_34px_-12px_rgba(0,0,0,0.55)]">
                🔥
              </div>
              <div className="flex min-w-0 flex-col gap-1.5">
                <span className="w-fit rounded-full bg-danger-soft px-2.5 py-0.5 font-mono text-[11px] font-semibold uppercase tracking-wide text-danger-deep">
                  Hot right now
                </span>
                <h2 className="truncate font-display text-2xl font-bold leading-tight text-ink-900">{data.content[0].title}</h2>
                <p className="text-sm text-ink-500">
                  {data.content[0].businessCompanyName} · ₹{data.content[0].ratePerThousandViewsInr} per 1,000 views
                </p>
              </div>
            </div>
          )}

          <div className={`grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3 ${isFetching ? "opacity-60" : ""}`}>
            {data.content.map((campaign) => (
              <CampaignCard key={campaign.id} campaign={campaign} />
            ))}
          </div>

          <Pagination page={data.page} totalPages={data.totalPages} last={data.last} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
