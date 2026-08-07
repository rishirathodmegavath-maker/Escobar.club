import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { contentApi } from "@/api/content";
import { FullPageSpinner } from "@/components/Spinner";
import { EmptyState } from "@/components/EmptyState";
import { ImageStackIcon } from "@/components/icons";
import { Button } from "@/components/Button";
import { ContentCard } from "@/features/content/ContentCard";

export function CreatorContentPage() {
  const { data: content, isLoading } = useQuery({
    queryKey: ["content", "me"],
    queryFn: () => contentApi.mine(0, 200),
  });

  if (isLoading) return <FullPageSpinner />;

  const items = content?.content ?? [];
  const published = items.filter((i) => i.status === "PUBLISHED").length;
  const awaitingReview = items.filter((i) => i.status === "SUBMITTED").length;
  const changesRequested = items.filter((i) => i.status === "CHANGES_REQUESTED").length;
  const featured = items.find((i) => i.status === "PUBLISHED") ?? items[0];

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">My submissions</h1>
        <p className="mt-1.5 text-ink-500">Track review feedback on the content you've submitted.</p>
      </div>

      {items.length === 0 ? (
        <EmptyState
          icon={<ImageStackIcon className="h-10 w-10" />}
          title="Nothing submitted yet"
          description="Browse campaigns and upload your first piece of content directly from a campaign's page."
          action={
            <Link to="/">
              <Button size="sm">Discover campaigns</Button>
            </Link>
          }
        />
      ) : (
        <>
          {featured && (
            <div className="hero-card flex items-center gap-6">
              <div className="flex h-[76px] w-[76px] shrink-0 items-center justify-center overflow-hidden rounded-2xl bg-gradient-to-br from-signal-500 to-signal-800 shadow-[0_18px_34px_-12px_rgba(0,0,0,0.55)]">
                {featured.mediaType === "IMAGE" ? (
                  <img src={featured.mediaUrl} alt="" className="h-full w-full object-cover" />
                ) : (
                  <span className="text-3xl">🎬</span>
                )}
              </div>
              <div className="flex min-w-0 flex-col gap-1.5">
                <span className="w-fit rounded-full bg-mint-soft px-2.5 py-0.5 font-mono text-[11px] font-semibold uppercase tracking-wide text-mint-deep">
                  {featured.status === "PUBLISHED" ? "Live" : "Latest submission"}
                </span>
                <h2 className="truncate font-display text-xl font-bold leading-tight text-ink-900">{featured.campaignTitle}</h2>
                <p className="text-sm text-ink-500">{featured.businessCompanyName}</p>
              </div>
            </div>
          )}

          <div className="grid grid-cols-3 gap-3.5">
            <div className="card-surface flex flex-col gap-1 p-4">
              <p className="text-[11px] font-semibold uppercase tracking-wide text-ink-400">Published</p>
              <p className="font-mono text-xl font-bold text-ink-900">{published}</p>
            </div>
            <div className="card-surface flex flex-col gap-1 p-4">
              <p className="text-[11px] font-semibold uppercase tracking-wide text-ink-400">Awaiting review</p>
              <p className="font-mono text-xl font-bold text-ink-900">{awaitingReview}</p>
            </div>
            <div className="card-surface flex flex-col gap-1 p-4">
              <p className="text-[11px] font-semibold uppercase tracking-wide text-ink-400">Changes requested</p>
              <p className="font-mono text-xl font-bold text-ink-900">{changesRequested}</p>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
            {items.map((item) => (
              <ContentCard key={item.id} content={item} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}
