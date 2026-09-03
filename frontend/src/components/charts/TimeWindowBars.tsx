import { useState } from "react";
import { Tabs } from "@/components/Tabs";
import type { PerformanceSummary, PerformanceWindow } from "@/types";

type WindowKey = "sevenDay" | "thirtyDay" | "ninetyDay" | "allTime";

const WINDOW_TABS: { label: string; value: WindowKey }[] = [
  { label: "7D", value: "sevenDay" },
  { label: "30D", value: "thirtyDay" },
  { label: "90D", value: "ninetyDay" },
  { label: "All time", value: "allTime" },
];

const numberFormatter = new Intl.NumberFormat("en-IN");

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-0.5">
      <span className="text-xs uppercase tracking-wide text-ink-400">{label}</span>
      <span className="font-display text-xl font-semibold text-ink-900">{value}</span>
    </div>
  );
}

// Small dependency-free bar comparison across the four pre-computed windows - no charting library
// needed for four bars, and switching windows is purely client-side (all four are in one payload).
export function TimeWindowBars({ summary }: { summary: PerformanceSummary }) {
  const [selected, setSelected] = useState<WindowKey>("thirtyDay");
  const windows: Record<WindowKey, PerformanceWindow> = summary;
  const active = windows[selected];
  const maxViews = Math.max(1, ...WINDOW_TABS.map((t) => windows[t.value].views));

  return (
    <div className="flex flex-col gap-5">
      <Tabs tabs={WINDOW_TABS} value={selected} onChange={setSelected} />

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <Stat label="Views" value={numberFormatter.format(active.views)} />
        <Stat label="Likes" value={numberFormatter.format(active.likes)} />
        <Stat label="Comments" value={numberFormatter.format(active.comments)} />
        <Stat label="Engagement rate" value={`${active.engagementRate}%`} />
      </div>

      <div className="flex items-end gap-3 border-t border-ink-100 pt-4" style={{ height: 96 }}>
        {WINDOW_TABS.map((tab) => {
          const heightPercent = Math.max(4, (windows[tab.value].views / maxViews) * 100);
          const isActive = tab.value === selected;
          return (
            <button
              key={tab.value}
              type="button"
              onClick={() => setSelected(tab.value)}
              className="focus-ring flex flex-1 flex-col items-center justify-end gap-1.5"
              title={`${numberFormatter.format(windows[tab.value].views)} views`}
            >
              <div
                className={
                  "w-full rounded-t-md transition-all " +
                  (isActive ? "bg-gradient-to-t from-signal-500 to-signal-800" : "bg-ink-100")
                }
                style={{ height: `${heightPercent}%` }}
              />
              <span className={"text-[11px] font-medium " + (isActive ? "text-ink-900" : "text-ink-400")}>{tab.label}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
