import { useQuery } from "@tanstack/react-query";
import { creatorsApi } from "@/api/creators";
import { Spinner } from "@/components/Spinner";
import { TickMeter } from "@/components/TickMeter";
import { Avatar } from "@/components/Avatar";

export function CreatorProfileInline({ creatorId }: { creatorId: number }) {
  const { data, isLoading } = useQuery({
    queryKey: ["creator", creatorId],
    queryFn: () => creatorsApi.getById(creatorId),
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-6">
        <Spinner />
      </div>
    );
  }
  if (!data) return null;

  return (
    <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
      <div className="col-span-full flex items-center gap-3">
        <Avatar name={data.displayName} imageUrl={data.profilePictureUrl} size={44} />
        <span className="font-display text-base font-semibold text-ink-900">{data.displayName}</span>
      </div>
      <div>
        <p className="text-xs font-semibold uppercase tracking-wide text-ink-400">Bio</p>
        <p className="mt-1 text-sm text-ink-600">{data.bio || "No bio provided."}</p>
        {data.niche && (
          <p className="mt-2 inline-block rounded-full bg-signal-50 px-2.5 py-1 text-xs font-medium text-signal-700">
            {data.niche}
          </p>
        )}
      </div>
      <div className="flex flex-col gap-4">
        <TickMeter
          label={`${data.followerCount.toLocaleString()} followers (self-reported)`}
          value={data.followerCount}
          max={100000}
          accent="gold"
        />
        {data.instagramProfileUrl && (
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-ink-400">Instagram</p>
            <a
              href={data.instagramProfileUrl}
              target="_blank"
              rel="noreferrer"
              className="mt-1 block break-all text-sm text-signal-600 hover:underline"
            >
              {data.instagramProfileUrl}
            </a>
          </div>
        )}
        {data.portfolioLinks.length > 0 && (
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-ink-400">Portfolio</p>
            <ul className="mt-1 flex flex-col gap-1">
              {data.portfolioLinks.map((link) => (
                <li key={link}>
                  <a href={link} target="_blank" rel="noreferrer" className="block break-all text-sm text-signal-600 hover:underline">
                    {link}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}
