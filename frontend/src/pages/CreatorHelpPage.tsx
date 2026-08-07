import { useState } from "react";
import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { Button } from "@/components/Button";
import { PersonPlusIcon, VideoIcon, EyeIcon, CoinIcon } from "@/components/icons";

const RATE_PER_1000_VIEWS_INR = 100;
const ELIGIBILITY_THRESHOLD_VIEWS = 5000;
const VIEW_STEP = 10_000;
const QUICK_VIEW_OPTIONS = [50_000, 100_000, 500_000, 1_000_000];

const inrFormatter = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });

function formatViewCount(views: number): string {
  if (views >= 1_000_000) return `${views / 1_000_000}M`;
  if (views >= 1_000) return `${views / 1000}k`;
  return `${views}`;
}

export function CreatorHelpPage() {
  const [totalViews, setTotalViews] = useState(100_000);
  const earningPotential = (Math.max(0, totalViews) / 1000) * RATE_PER_1000_VIEWS_INR;

  return (
    <div className="min-h-screen bg-paper px-4 py-12 sm:py-16">
      <div className="mx-auto max-w-4xl">
        <Link to="/login" className="focus-ring mb-8 inline-flex items-center gap-1.5 text-sm text-ink-400 hover:text-ink-700">
          ← Back to sign in
        </Link>

        <div className="mb-10 text-center">
          <h1 className="font-display text-3xl font-semibold text-ink-900">For Creators</h1>
          <p className="mt-1.5 text-ink-500">Earning from your content is no longer a guessing game.</p>
        </div>

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <div className="card-surface p-6">
            <div className="mb-5 flex items-center justify-between gap-3">
              <h2 className="font-display text-lg font-semibold text-ink-900">Earning calculator</h2>
              <span className="shrink-0 rounded-full bg-ink-100 px-2.5 py-1 text-xs font-medium uppercase tracking-wide text-ink-500">
                Estimate only
              </span>
            </div>

            <label className="mb-1.5 block text-sm font-medium text-ink-700" htmlFor="total-views">
              Total views
            </label>
            <div className="mb-5 flex items-center gap-2">
              <Button
                type="button"
                variant="secondary"
                aria-label="Decrease views"
                onClick={() => setTotalViews((v) => Math.max(0, v - VIEW_STEP))}
                className="h-[42px] w-[42px] shrink-0 px-0 text-lg"
              >
                −
              </Button>
              <input
                id="total-views"
                type="number"
                min={0}
                step={VIEW_STEP}
                value={totalViews}
                onChange={(e) => setTotalViews(Math.max(0, Math.round(Number(e.target.value) || 0)))}
                className="focus-ring w-full rounded-lg border border-ink-200 bg-surface-input px-3.5 py-2.5 text-center font-mono text-sm text-ink-900"
              />
              <Button
                type="button"
                variant="secondary"
                aria-label="Increase views"
                onClick={() => setTotalViews((v) => v + VIEW_STEP)}
                className="h-[42px] w-[42px] shrink-0 px-0 text-lg"
              >
                +
              </Button>
            </div>

            <div className="mb-5 rounded-xl bg-signal-soft p-5">
              <div className="flex items-center justify-between gap-3">
                <span className="text-sm text-ink-600">Earning potential</span>
                <span className="font-display text-2xl font-semibold text-signal-deep">{inrFormatter.format(earningPotential)}</span>
              </div>
              <div className="mt-4 grid grid-cols-2 gap-3">
                <div className="rounded-lg bg-surface-hover p-3">
                  <p className="text-xs uppercase tracking-wide text-ink-400">Per 1K views</p>
                  <p className="font-mono text-sm font-semibold text-ink-900">{inrFormatter.format(RATE_PER_1000_VIEWS_INR)}</p>
                </div>
                <div className="rounded-lg bg-surface-hover p-3">
                  <p className="text-xs uppercase tracking-wide text-ink-400">Total payout</p>
                  <p className="font-mono text-sm font-semibold text-ink-900">{inrFormatter.format(earningPotential)}</p>
                </div>
              </div>
            </div>

            <div className="flex flex-wrap gap-2">
              {QUICK_VIEW_OPTIONS.map((views) => (
                <button
                  key={views}
                  type="button"
                  onClick={() => setTotalViews(views)}
                  className="focus-ring rounded-full border border-ink-200 bg-surface px-3 py-1.5 font-mono text-xs font-medium text-ink-600 transition-colors hover:border-signal-300 hover:bg-signal-soft hover:text-signal-deep"
                >
                  {formatViewCount(views)} views
                </button>
              ))}
            </div>
          </div>

          <div className="flex flex-col gap-6">
            <div className="card-surface p-6">
              <h2 className="mb-5 font-display text-lg font-semibold text-ink-900">How it works</h2>
              <ol className="flex flex-col gap-4">
                <CalculatorStep n={1} title="Enter your monthly views" desc="Get an instant estimate based on your audience size." />
                <CalculatorStep
                  n={2}
                  title="See your earning potential"
                  desc={`Our platform pays ${inrFormatter.format(RATE_PER_1000_VIEWS_INR)} per 1,000 views with transparent, creator-friendly rates.`}
                />
                <CalculatorStep
                  n={3}
                  title="Start creating and earning"
                  desc="Join our platform to start monetizing your content with fair, transparent payouts."
                />
              </ol>
            </div>
            <div className="rounded-lg border border-gold-200 bg-gold-soft px-4 py-3 text-sm text-gold-deep">
              <span className="font-semibold">Note:</span> These are estimated earnings. Actual payout depends on the rate
              set by each campaign, and may vary based on content quality, view source, and engagement.
            </div>
          </div>
        </div>

        <div className="my-12 border-t border-ink-100" />

        <div className="mb-8 text-center">
          <h2 className="font-display text-2xl font-semibold text-ink-900">How it works</h2>
          <p className="mt-1.5 text-ink-500">From joining to getting paid — here's your journey to earning with every view.</p>
        </div>

        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
          <JourneyStep
            icon={<PersonPlusIcon className="h-6 w-6" />}
            iconClassName="bg-signal-soft text-signal-deep"
            title="Join the Cartel"
            description="Sign up and complete your creator profile. Connect your social accounts and showcase your content style."
            footnote="Verified by the businesses you work with"
          />
          <JourneyStep
            icon={<VideoIcon className="h-6 w-6" />}
            iconClassName="bg-gold-soft text-gold-deep"
            title="Create content"
            description="Browse open campaigns, choose brands you love, and submit content directly — no waiting on approval to get started."
            footnote="Full creative freedom"
          />
          <JourneyStep
            icon={<EyeIcon className="h-6 w-6" />}
            iconClassName="bg-alert-soft text-alert-deep"
            title="Get views"
            description="Once your content is published, sync its performance any time to track likes, comments, and views."
            footnote="Views tracked on demand"
          />
          <JourneyStep
            icon={<CoinIcon className="h-6 w-6" />}
            iconClassName="bg-signal-soft text-signal-deep"
            title="Earn money"
            description={`Get paid per view after crossing the ${ELIGIBILITY_THRESHOLD_VIEWS.toLocaleString("en-IN")}-view threshold. The business settles your payout once you're eligible.`}
            footnote="Transparent, per-campaign rates"
          />
        </div>
      </div>
    </div>
  );
}

function CalculatorStep({ n, title, desc }: { n: number; title: string; desc: string }) {
  return (
    <li className="flex gap-3">
      <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-signal-500 font-mono text-xs font-bold text-white">
        {n}
      </span>
      <div>
        <p className="font-medium text-ink-900">{title}</p>
        <p className="mt-0.5 text-sm text-ink-500">{desc}</p>
      </div>
    </li>
  );
}

function JourneyStep({
  icon,
  iconClassName,
  title,
  description,
  footnote,
}: {
  icon: ReactNode;
  iconClassName: string;
  title: string;
  description: string;
  footnote: string;
}) {
  return (
    <div className="card-surface p-6 text-center">
      <div className={`mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl2 ${iconClassName}`}>{icon}</div>
      <h3 className="font-display text-base font-semibold text-ink-900">{title}</h3>
      <p className="mt-2 text-sm text-ink-500">{description}</p>
      <p className="mt-3 text-xs font-semibold text-signal-700">{footnote}</p>
    </div>
  );
}
