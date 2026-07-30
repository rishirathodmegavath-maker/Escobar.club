import clsx from "clsx";
import type { LeaderboardEntry } from "@/types";
import { Avatar } from "@/components/Avatar";

const compactNumber = new Intl.NumberFormat("en-US", { notation: "compact", maximumFractionDigits: 1 });

function RankBadge({ rank }: { rank: number }) {
  if (rank === 1) {
    return (
      <span className="flex h-7 w-7 items-center justify-center rounded-full bg-gold-400 font-mono text-xs font-bold text-ink-950">
        {rank}
      </span>
    );
  }
  if (rank <= 3) {
    return (
      <span className="flex h-7 w-7 items-center justify-center rounded-full bg-ink-100 font-mono text-xs font-bold text-ink-700">
        {rank}
      </span>
    );
  }
  return <span className="flex h-7 w-7 items-center justify-center font-mono text-sm font-medium text-ink-400">{rank}</span>;
}

export function LeaderboardRow({ entry }: { entry: LeaderboardEntry }) {
  return (
    <tr className={clsx("border-t border-ink-100", entry.rank <= 3 && "bg-paper-100/60")}>
      <td className="py-3 pl-4 pr-2">
        <RankBadge rank={entry.rank} />
      </td>
      <td className="py-3 pr-4">
        <div className="flex items-center gap-3">
          <Avatar name={entry.creatorDisplayName} imageUrl={entry.creatorProfilePictureUrl} size={32} />
          <span className="font-medium text-ink-900">{entry.creatorDisplayName}</span>
        </div>
      </td>
      <td className="py-3 pr-4 text-right font-mono tabular-nums text-ink-600">{entry.publishedContentCount}</td>
      <td className="py-3 pr-4 text-right font-mono text-base font-semibold tabular-nums text-ink-900">
        {compactNumber.format(entry.totalViews)}
      </td>
    </tr>
  );
}
