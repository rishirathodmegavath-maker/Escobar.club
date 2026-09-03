import clsx from "clsx";

export function ProgressBar({ percent, tone = "signal" }: { percent: number; tone?: "signal" | "gold" | "danger" }) {
  const clamped = Math.max(0, Math.min(100, percent));
  const toneClass =
    tone === "danger" ? "bg-danger-500" : tone === "gold" ? "bg-gold-500" : "bg-gradient-to-r from-signal-500 to-signal-800";
  return (
    <div className="h-2 w-full overflow-hidden rounded-full bg-ink-100">
      <div className={clsx("h-full rounded-full transition-all", toneClass)} style={{ width: `${clamped}%` }} />
    </div>
  );
}
