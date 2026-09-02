import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { businessesApi } from "@/api/businesses";
import { Avatar } from "@/components/Avatar";
import { Button } from "@/components/Button";
import { FullPageSpinner } from "@/components/Spinner";
import { EditIcon } from "@/components/icons";

function ProfileField({ label, value, href }: { label: string; value: string | null; href?: string | null }) {
  return (
    <div>
      <p className="text-xs font-semibold uppercase tracking-wide text-ink-400">{label}</p>
      {value ? (
        href ? (
          <a href={href} target="_blank" rel="noreferrer" className="focus-ring mt-1 block text-sm text-signal-600 hover:underline">
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

  if (isLoading) return <FullPageSpinner />;
  if (!data) return null;

  return (
    <div className="mx-auto max-w-2xl">
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl font-semibold text-ink-900">Company profile</h1>
          <p className="mt-1.5 text-ink-500">This is your public page — creators will see this before applying.</p>
        </div>
        <Link to="/business/profile/edit">
          <Button size="sm">
            <EditIcon className="h-4 w-4" />
            Edit Profile
          </Button>
        </Link>
      </div>

      <div className="card-surface flex flex-col gap-6 p-7">
        <div className="flex items-center gap-4">
          <Avatar name={data.companyName} imageUrl={data.logoUrl} size={64} />
          <div>
            <h2 className="font-display text-xl font-semibold text-ink-900">{data.companyName}</h2>
            {data.industry && <p className="text-sm text-ink-500">{data.industry}</p>}
          </div>
        </div>

        <div className="grid grid-cols-1 gap-5 border-t border-ink-100 pt-6 sm:grid-cols-2">
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
    </div>
  );
}
