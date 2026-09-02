import { useState } from "react";
import clsx from "clsx";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { businessesApi } from "@/api/businesses";
import { Avatar } from "@/components/Avatar";
import { Button } from "@/components/Button";
import { FullPageSpinner } from "@/components/Spinner";
import { ImageViewerModal } from "@/components/ImageViewerModal";
import { EditIcon } from "@/components/icons";

function ProfileField({ label, value, href }: { label: string; value: string | null; href?: string | null }) {
  return (
    <div>
      <p className="text-xs font-semibold uppercase tracking-wide text-ink-400">{label}</p>
      {value ? (
        href ? (
          <a href={href} target="_blank" rel="noreferrer" className="focus-ring mt-1 block break-all text-sm text-signal-600 hover:underline">
            {value}
          </a>
        ) : (
          <p className="mt-1 text-sm text-ink-800">{value}</p>
        )
      ) : (
        <p className="mt-1 text-sm text-ink-400">Not set</p>
      )}
    </div>
  );
}

export function BusinessProfileViewPage() {
  const { data, isLoading } = useQuery({ queryKey: ["business", "me"], queryFn: businessesApi.getMine });
  const [viewerOpen, setViewerOpen] = useState(false);

  if (isLoading) return <FullPageSpinner />;
  if (!data) return null;

  return (
    <div className="mx-auto max-w-2xl">
      <div className="hero-card flex flex-col gap-5 p-7 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-4">
          <button
            type="button"
            onClick={() => data.logoUrl && setViewerOpen(true)}
            className={clsx("focus-ring shrink-0 rounded-avatar", data.logoUrl && "cursor-zoom-in")}
            aria-label={data.logoUrl ? "View company logo" : undefined}
          >
            <Avatar name={data.companyName} imageUrl={data.logoUrl} size={72} />
          </button>
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="font-display text-2xl font-semibold text-ink-900">{data.companyName}</h1>
              <span className="rounded-full bg-signal-50 px-2.5 py-0.5 text-xs font-semibold text-signal-700">Business</span>
            </div>
            {data.industry && <p className="mt-0.5 text-sm text-ink-500">{data.industry}</p>}
          </div>
        </div>
        <Link to="/business/profile/edit" className="shrink-0">
          <Button size="sm">
            <EditIcon className="h-4 w-4" />
            Edit Profile
          </Button>
        </Link>
      </div>
      <p className="mt-3 text-sm text-ink-500">This is your public page — creators will see this before applying.</p>

      <div className="card-surface mt-4 flex flex-col gap-6 p-7">
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
          <ProfileField label="GST Number" value={data.gstNumber} />
          <ProfileField label="Contact Person" value={data.contactPersonName} />
          <ProfileField label="Mobile Number" value={data.mobileNumber} />
          <ProfileField label="Website" value={data.website} href={data.website} />
        </div>

        <div className="border-t border-ink-100 pt-6">
          <p className="text-xs font-semibold uppercase tracking-wide text-ink-400">Public description</p>
          <p className="mt-1 whitespace-pre-wrap text-sm text-ink-600">{data.description || "No description added yet."}</p>
        </div>
      </div>

      {viewerOpen && data.logoUrl && (
        <ImageViewerModal imageUrl={data.logoUrl} alt={data.companyName} onClose={() => setViewerOpen(false)} />
      )}
    </div>
  );
}
