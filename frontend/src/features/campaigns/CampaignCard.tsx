import { Link } from "react-router-dom";
import type { Campaign } from "@/types";
import { Avatar } from "@/components/Avatar";
import { StatusPill } from "@/components/StatusPill";
import { formatCompactInr } from "@/utils/formatIndianNumber";

const dateFormatter = new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short", year: "numeric" });
const inrFormatter = { format: (value: number) => formatCompactInr(value, 0) };

function formatDateRange(startDate: string, endDate: string): string {
  return `${dateFormatter.format(new Date(startDate))} – ${dateFormatter.format(new Date(endDate))}`;
}

export function CampaignCard({ campaign }: { campaign: Campaign }) {
  return (
    <Link
      to={`/campaigns/${campaign.id}`}
      className="card-surface group flex flex-col gap-4 p-5 hover:-translate-y-0.5"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <Avatar name={campaign.businessCompanyName} imageUrl={campaign.businessLogoUrl} size={36} />
          <div className="min-w-0">
            <h3 className="truncate font-display text-base font-semibold text-ink-900 group-hover:text-signal-700">
              {campaign.title}
            </h3>
            <p className="truncate text-xs uppercase tracking-wide text-ink-400">{campaign.businessCompanyName}</p>
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

      {campaign.description && <p className="line-clamp-3 text-sm text-ink-500">{campaign.description}</p>}

      <div className="mt-auto flex items-center justify-between border-t border-ink-100 pt-3 font-mono text-xs text-ink-400">
        <span>{formatDateRange(campaign.publishStartAt, campaign.publishEndAt)}</span>
        <span className="font-semibold text-gold-deep">
          {inrFormatter.format(campaign.ratePerThousandViewsInr)} / 1,000 views
        </span>
      </div>
    </Link>
  );
}
