import type { ReactNode } from "react";

export function EmptyState({
  icon,
  title,
  description,
  action,
}: {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-xl2 border border-dashed border-surface-border bg-surface px-8 py-16 text-center">
      {icon && <div className="text-ink-300">{icon}</div>}
      <h3 className="font-display text-lg text-ink-800">{title}</h3>
      {description && <p className="max-w-sm text-sm text-ink-400">{description}</p>}
      {action}
    </div>
  );
}
