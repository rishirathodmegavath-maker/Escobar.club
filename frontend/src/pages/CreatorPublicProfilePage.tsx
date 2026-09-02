import { useNavigate, useParams } from "react-router-dom";
import { CreatorProfileInline } from "@/features/content/CreatorProfileInline";

export function CreatorPublicProfilePage() {
  const { creatorId } = useParams<{ creatorId: string }>();
  const navigate = useNavigate();
  const id = Number(creatorId);

  return (
    <div className="mx-auto max-w-2xl">
      <div className="mb-6">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="focus-ring text-sm font-medium text-signal-600 hover:text-signal-700"
        >
          ← Back
        </button>
        <h1 className="mt-2 font-display text-3xl font-semibold text-ink-900">Creator profile</h1>
      </div>

      <div className="card-surface p-7">
        <CreatorProfileInline creatorId={id} />
      </div>
    </div>
  );
}
