import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/auth/AuthContext";
import { creatorsApi } from "@/api/creators";
import { Avatar } from "@/components/Avatar";
import { Button } from "@/components/Button";
import { FullPageSpinner } from "@/components/Spinner";
import { EditIcon } from "@/components/icons";
import { CreatorProfileInline } from "@/features/content/CreatorProfileInline";

export function CreatorProfileViewPage() {
  const { user } = useAuth();
  const { data, isLoading } = useQuery({
    queryKey: ["creator", "me"],
    queryFn: creatorsApi.getMine,
    enabled: !!user,
  });

  if (!user || isLoading) return <FullPageSpinner />;
  if (!data) return null;

  return (
    <div className="mx-auto max-w-2xl">
      <div className="hero-card flex flex-col gap-5 p-7 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-4">
          <Avatar name={data.displayName} imageUrl={data.profilePictureUrl} size={72} />
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="font-display text-2xl font-semibold text-ink-900">{data.displayName}</h1>
              <span className="rounded-full bg-signal-50 px-2.5 py-0.5 text-xs font-semibold text-signal-700">Creator</span>
            </div>
            {data.niche && <p className="mt-0.5 text-sm text-ink-500">{data.niche}</p>}
          </div>
        </div>
        <Link to="/creator/profile/edit" className="shrink-0">
          <Button size="sm">
            <EditIcon className="h-4 w-4" />
            Edit Profile
          </Button>
        </Link>
      </div>
      <p className="mt-3 text-sm text-ink-500">This is what businesses see when reviewing your content submissions.</p>

      <div className="card-surface mt-4 flex flex-col gap-4 p-7">
        <CreatorProfileInline creatorId={user.id} hideHeader />
      </div>
    </div>
  );
}
