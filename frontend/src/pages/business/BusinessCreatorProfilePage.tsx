import { Link, useParams } from "react-router-dom";
import { CreatorProfileInline } from "@/features/content/CreatorProfileInline";
import { KycReviewPanel } from "@/features/kyc/KycReviewPanel";

export function BusinessCreatorProfilePage() {
  const { creatorId } = useParams<{ creatorId: string }>();
  const id = Number(creatorId);

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-6">
      <div>
        <Link to="/business/content" className="focus-ring text-sm font-medium text-signal-600 hover:text-signal-700">
          ← Back to review queue
        </Link>
        <h1 className="mt-2 font-display text-3xl font-semibold text-ink-900">Creator profile</h1>
        <p className="mt-1.5 text-ink-500">Profile details and KYC status for this creator.</p>
      </div>

      <div className="card-surface flex flex-col gap-4 p-6">
        <CreatorProfileInline creatorId={id} />
        <KycReviewPanel creatorId={id} />
      </div>
    </div>
  );
}
