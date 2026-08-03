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
          className="focus-ring w-full max-w-sm rounded-[10px] border border-ink-200 bg-white px-4 py-2.5 text-sm placeholder:text-ink-300 sm:max-w-xs"
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
