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
  { label: "🟢 Live", value: "LIVE" },
  { label: "⏳ Upcoming", value: "UPCOMING" },
];

const CATEGORY_DESCRIPTIONS: Record<CampaignCategory, string> = {
  HOT: "High-priority campaigns that need creators immediately.",
  LIVE: "Campaigns currently open — submit your content directly, no approval needed to get started.",
  UPCOMING: "Scheduled to go live soon. You can view these now, but submissions open once they're Live.",
};

const CATEGORY_EMPTY_TEXT: Record<CampaignCategory, string> = {
  HOT: "No high-priority campaigns right now — check the Live tab for everything currently open.",
  LIVE: "No campaigns are open for submissions right now. Check back soon.",
  UPCOMING: "No campaigns are scheduled to go live soon.",
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
