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

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">My submissions</h1>
        <p className="mt-1.5 text-ink-500">Track review feedback on the content you've submitted.</p>
      </div>

      {!content || content.content.length === 0 ? (
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
        <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
          {content.content.map((item) => (
            <ContentCard key={item.id} content={item} />
          ))}
        </div>
      )}
    </div>
  );
}
