import { useParams } from "react-router-dom";
import { CreatorProfileInline } from "@/features/content/CreatorProfileInline";

export function CreatorPublicProfilePage() {
  const { creatorId } = useParams<{ creatorId: string }>();
  const id = Number(creatorId);

  return (
    <div className="mx-auto max-w-2xl">
      <div className="mb-6">
        <h1 className="font-display text-3xl font-semibold text-ink-900">Creator profile</h1>
      </div>

      <div className="card-surface p-7">
        <CreatorProfileInline creatorId={id} />
      </div>
    </div>
  );
}
