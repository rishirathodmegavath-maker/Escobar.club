import { Link } from "react-router-dom";
import { ChevronRightIcon } from "@/components/icons";

export function StatCard({
  label,
  value,
  sublabel,
  to,
}: {
  label: string;
  value: string;
  sublabel?: string;
  to: string;
}) {
  return (
    <Link
      to={to}
      className="card-surface focus-ring group flex flex-col gap-1 p-6 transition-transform hover:-translate-y-0.5"
    >
      <div className="flex items-center justify-between gap-2">
        <span className="text-xs font-medium uppercase tracking-wide text-ink-400">{label}</span>
        <ChevronRightIcon className="h-4 w-4 text-ink-300 transition-transform group-hover:translate-x-0.5 group-hover:text-signal-500" />
      </div>
      <span className="font-display text-3xl font-semibold text-ink-900">{value}</span>
      {sublabel && <span className="text-xs text-ink-400">{sublabel}</span>}
    </Link>
  );
}
