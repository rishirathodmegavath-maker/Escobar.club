import { Link } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { Button } from "@/components/Button";
import { EditIcon } from "@/components/icons";
import { CreatorProfileInline } from "@/features/content/CreatorProfileInline";

export function CreatorProfileViewPage() {
  const { user } = useAuth();
  if (!user) return null;

  return (
    <div className="mx-auto max-w-2xl">
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl font-semibold text-ink-900">My profile</h1>
          <p className="mt-1.5 text-ink-500">This is what businesses see when reviewing your content submissions.</p>
        </div>
        <Link to="/creator/profile/edit">
          <Button size="sm">
            <EditIcon className="h-4 w-4" />
            Edit Profile
          </Button>
        </Link>
      </div>

      <div className="card-surface flex flex-col gap-4 p-7">
        <CreatorProfileInline creatorId={user.id} />
      </div>
    </div>
  );
}
