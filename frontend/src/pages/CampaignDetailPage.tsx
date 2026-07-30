import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { campaignsApi } from "@/api/campaigns";
import { useAuth } from "@/auth/AuthContext";
import { FullPageSpinner } from "@/components/Spinner";
import { Avatar } from "@/components/Avatar";
import { ContentSubmitCard } from "@/features/content/ContentSubmitCard";

const dateFormatter = new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "long", year: "numeric" });
const inrFormatter = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });

export function CampaignDetailPage() {
  const { id } = useParams();
  const campaignId = Number(id);
  const { user } = useAuth();

  const { data: campaign, isLoading } = useQuery({
    queryKey: ["campaign", campaignId],
    queryFn: () => campaignsApi.getById(campaignId),
    enabled: Number.isFinite(campaignId),
  });

  if (isLoading) return <FullPageSpinner />;
  if (!campaign) return <p className="text-ink-500">Campaign not found.</p>;

  return (
    <div className="mx-auto max-w-3xl">
      <div className="card-surface mb-6 flex items-start gap-5 p-7">
        <Avatar name={campaign.businessCompanyName} imageUrl={campaign.businessLogoUrl} size={64} className="text-2xl" />
        <div className="min-w-0 flex-1">
          <h1 className="font-display text-2xl font-semibold text-ink-900">{campaign.title}</h1>
          <p className="text-sm uppercase tracking-wide text-ink-400">{campaign.businessCompanyName}</p>
          <div className="mt-3 flex flex-wrap gap-4 font-mono text-sm text-ink-600">
            <span>
              <span className="font-sans text-ink-400">Runs</span>{" "}
              {dateFormatter.format(new Date(campaign.startDate))} – {dateFormatter.format(new Date(campaign.endDate))}
            </span>
            <span className="font-semibold text-gold-deep">
              {inrFormatter.format(campaign.ratePerThousandViewsInr)} / 1,000 views
            </span>
          </div>
        </div>
        <div className="flex shrink-0 flex-col items-end gap-1.5">
          {campaign.hot && (
            <span className="rounded-full bg-alert-soft px-2.5 py-1 font-mono text-xs font-semibold uppercase tracking-wide text-alert-deep">
              🔥 Hot
            </span>
          )}
          {campaign.acceptingSubmissions ? (
            <span className="rounded-full bg-signal-soft px-2.5 py-1 font-mono text-xs font-semibold uppercase tracking-wide text-signal-deep">
              Live
            </span>
          ) : campaign.status === "STARTING_SOON" ? (
            <span className="rounded-full bg-gold-soft px-2.5 py-1 font-mono text-xs font-semibold uppercase tracking-wide text-gold-deep">
              Upcoming
            </span>
          ) : (
            <span className="rounded-full bg-ink-100 px-2.5 py-1 font-mono text-xs font-semibold uppercase tracking-wide text-ink-500">
              Closed
            </span>
          )}
        </div>
      </div>

      {campaign.description && (
        <div className="card-surface mb-6 p-7">
          <h2 className="mb-2 font-display text-lg font-semibold text-ink-800">About this campaign</h2>
          <p className="whitespace-pre-line text-sm leading-relaxed text-ink-600">{campaign.description}</p>
        </div>
      )}

      {user?.role === "CREATOR" && (
        <div className="card-surface p-7">
          <h2 className="mb-4 font-display text-lg font-semibold text-ink-800">Submit content</h2>
          {campaign.acceptingSubmissions ? (
            <>
              <p className="mb-4 text-sm text-ink-500">
                Upload a piece of content for this campaign. The business will review it before it goes live — you can
                submit as many pieces as you like.
              </p>
              <ContentSubmitCard campaign={campaign} />
            </>
          ) : campaign.status === "STARTING_SOON" ? (
            <p className="text-sm text-ink-500">This campaign hasn't gone live yet — check back once it's Live to submit content.</p>
          ) : (
            <p className="text-sm text-ink-500">This campaign is closed and no longer accepting content submissions.</p>
          )}
        </div>
      )}
    </div>
  );
}
