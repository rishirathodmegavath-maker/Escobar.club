import clsx from "clsx";

interface TabsProps<T> {
  tabs: { label: string; value: T }[];
  value: T;
  onChange: (value: T) => void;
  className?: string;
}

export function Tabs<T>({ tabs, value, onChange, className }: TabsProps<T>) {
  return (
    <div className={clsx("flex flex-wrap gap-2 self-start rounded-[10px] border border-surface-border bg-surface p-1", className)}>
      {tabs.map((tab) => (
        <button
          key={tab.label}
          onClick={() => onChange(tab.value)}
          className={clsx(
            "focus-ring rounded-[8px] px-4 py-1.5 text-sm font-medium transition-colors",
            value === tab.value
              ? "bg-gradient-to-br from-signal-500 to-signal-800 text-white shadow-[0_4px_18px_rgba(250,35,59,0.4)]"
              : "text-ink-400 hover:text-ink-700",
          )}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}
