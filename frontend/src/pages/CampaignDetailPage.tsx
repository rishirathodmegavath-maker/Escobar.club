import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { campaignsApi } from "@/api/campaigns";
import { kycApi } from "@/api/kyc";
import { useAuth } from "@/auth/AuthContext";
import { FullPageSpinner } from "@/components/Spinner";
import { Avatar } from "@/components/Avatar";
import { StatusPill } from "@/components/StatusPill";
import { ContentSubmitCard } from "@/features/content/ContentSubmitCard";
import { KycGateNotice } from "@/features/content/KycGateNotice";
import type { Campaign, CreatorKycProfile } from "@/types";

const dateFormatter = new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "long", year: "numeric" });
const inrFormatter = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });

function daysUntil(dateStr: string): number {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const target = new Date(dateStr);
  target.setHours(0, 0, 0, 0);
  return Math.round((target.getTime() - today.getTime()) / 86_400_000);
}

function SubmissionPanel({
  campaign,
  kyc,
  kycLoading,
}: {
  campaign: Campaign;
  kyc: CreatorKycProfile | undefined;
  kycLoading: boolean;
}) {
  if (campaign.status === "UPCOMING") {
    const startsIn = daysUntil(campaign.publishStartAt);
    const deadlineIn = daysUntil(campaign.submissionDeadline);
    return (
      <>
        <div className="mb-4 flex flex-wrap gap-4 font-mono text-sm text-ink-600">
          <span>
            <span className="font-sans text-ink-400">Campaign starts in</span> {startsIn} day{startsIn === 1 ? "" : "s"}
          </span>
          <span>
            <span className="font-sans text-ink-400">Submission closes in</span> {deadlineIn} day{deadlineIn === 1 ? "" : "s"}
          </span>
        </div>
        {campaign.acceptingSubmissions ? (
          kycLoading ? null : kyc?.eligibleToParticipate ? (
            <>
              <p className="mb-4 text-sm text-ink-500">
                Upload a piece of content for this campaign. The business will review it before it goes live — you can
                submit as many pieces as you like.
              </p>
              <ContentSubmitCard campaign={campaign} />
            </>
          ) : (
            <KycGateNotice kyc={kyc} />
          )
        ) : (
          <p className="text-sm text-ink-500">
            {campaign.submissionClosedReason ?? "Submissions have not opened yet — check back soon to upload content."}
          </p>
        )}
      </>
    );
  }

  if (campaign.status === "LIVE") {
    return (
      <div className="flex flex-wrap items-center gap-3">
        <StatusPill status="LIVE" />
        <span className="text-sm text-ink-500">Views are being counted — submissions are closed for this campaign.</span>
      </div>
    );
  }

  if (campaign.status === "COMPLETED") {
    return <p className="text-sm text-ink-500">Campaign ended. No further submissions or views are being counted.</p>;
  }

  return <p className="text-sm text-ink-500">This campaign isn't accepting submissions right now.</p>;
}

export function CampaignDetailPage() {
  const { id } = useParams();
  const campaignId = Number(id);
  const { user } = useAuth();
  const { data: campaign, isLoading } = useQuery({
    queryKey: ["campaign", campaignId],
    queryFn: () => campaignsApi.getById(campaignId),
    enabled: Number.isFinite(campaignId),
  });
  const { data: kyc, isLoading: kycLoading } = useQuery({
    queryKey: ["kyc", "me"],
    queryFn: kycApi.mine,
    enabled: user?.role === "CREATOR",
    retry: false,
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
              <span className="font-sans text-ink-400">Live</span>{" "}
              {dateFormatter.format(new Date(campaign.publishStartAt))} – {dateFormatter.format(new Date(campaign.publishEndAt))}
            </span>
            <span className="font-semibold text-gold-deep">
              {inrFormatter.format(campaign.ratePerThousandViewsInr)} / 1,000 views
            </span>
          </div>
        </div>
        <div className="flex shrink-0 flex-col items-end gap-1.5">
          {campaign.adminDisplayStatus ? (
            <StatusPill status={campaign.adminDisplayStatus} />
          ) : (
            <>
              {campaign.hot && (
                <span className="rounded-full bg-alert-soft px-2.5 py-1 font-mono text-xs font-semibold uppercase tracking-wide text-alert-deep">
                  🔥 Hot
                </span>
              )}
              <StatusPill status={campaign.status} />
            </>
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
          <SubmissionPanel campaign={campaign} kyc={kyc} kycLoading={kycLoading} />
        </div>
      )}
    </div>
  );
}
