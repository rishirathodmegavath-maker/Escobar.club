import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { leaderboardApi } from "@/api/leaderboard";
import { useAuth } from "@/auth/AuthContext";
import { FullPageSpinner } from "@/components/Spinner";
import { Pagination } from "@/components/Pagination";
import { LeaderboardTable } from "@/features/leaderboard/LeaderboardTable";

export function BusinessLeaderboardPage() {
  const { user } = useAuth();
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["leaderboard", "business", user?.id, page],
    queryFn: () => leaderboardApi.business(user!.id, page),
    enabled: !!user,
  });

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">Your creator leaderboard</h1>
        <p className="mt-1.5 text-ink-500">Creators ranked by total views across content published for your business.</p>
      </div>

      {isLoading || !data ? <FullPageSpinner /> : <LeaderboardTable entries={data.content} />}

      {data && <Pagination page={data.page} totalPages={data.totalPages} last={data.last} onPageChange={setPage} />}
    </div>
  );
}
