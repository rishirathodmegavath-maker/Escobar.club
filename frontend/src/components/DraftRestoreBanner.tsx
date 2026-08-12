function timeAgo(iso: string): string {
  const seconds = Math.max(0, Math.floor((Date.now() - new Date(iso).getTime()) / 1000));
  if (seconds < 60) return "just now";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

export function DraftRestoreBanner({
  updatedAt,
  onRestore,
  onDiscard,
}: {
  updatedAt: string;
  onRestore: () => void;
  onDiscard: () => void;
}) {
  return (
    <div className="mb-6 flex items-center justify-between gap-4 rounded-xl border border-gold-200 bg-gold-soft px-4 py-3 text-sm text-gold-deep">
      <span>You have unsaved work from {timeAgo(updatedAt)}. Want to pick up where you left off?</span>
      <div className="flex shrink-0 gap-2">
        <button
          type="button"
          onClick={onRestore}
          className="focus-ring rounded-lg bg-gold-500 px-3 py-1.5 text-xs font-semibold text-white hover:bg-gold-600"
        >
          Restore
        </button>
        <button
          type="button"
          onClick={onDiscard}
          className="focus-ring rounded-lg px-3 py-1.5 text-xs font-semibold text-gold-deep hover:bg-gold-100"
        >
          Discard
        </button>
      </div>
    </div>
  );
}
