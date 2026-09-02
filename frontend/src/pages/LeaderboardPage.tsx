import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { leaderboardApi } from "@/api/leaderboard";
import { FullPageSpinner } from "@/components/Spinner";
import { Pagination } from "@/components/Pagination";
import { LeaderboardTable } from "@/features/leaderboard/LeaderboardTable";

export function LeaderboardPage() {
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["leaderboard", "global", page],
    queryFn: () => leaderboardApi.global(page),
  });

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">Global leaderboard</h1>
        <p className="mt-1.5 text-ink-500">Creators ranked by total views across every published piece of content.</p>
      </div>

      {isLoading || !data ? <FullPageSpinner /> : <LeaderboardTable entries={data.content} />}

      {data && <Pagination page={data.page} totalPages={data.totalPages} last={data.last} onPageChange={setPage} />}
    </div>
  );
}
