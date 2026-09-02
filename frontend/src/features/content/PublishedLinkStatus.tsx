import type { ContentRecord } from "@/types";

// Shown on both the creator's own content page and the business review queue - the live link
// only appears once an admin has approved it (status PUBLISHED); before that it's pending.
export function PublishedLinkStatus({ content }: { content: ContentRecord }) {
  if (content.status === "PENDING_LINK_REVIEW") {
    return (
      <div className="border-t border-ink-100 pt-4">
        <p className="text-xs font-semibold uppercase tracking-wide text-ink-400">Live link</p>
        <p className="mt-1 text-sm text-ink-600">Link submitted — awaiting admin approval before it goes live.</p>
      </div>
    );
  }

  if (content.status === "PUBLISHED" && content.postUrl) {
    return (
      <div className="border-t border-ink-100 pt-4">
        <p className="text-xs font-semibold uppercase tracking-wide text-ink-400">Live link</p>
        <a
          href={content.postUrl}
          target="_blank"
          rel="noreferrer"
          className="focus-ring mt-1 inline-block break-all text-sm text-signal-600 hover:text-signal-700 hover:underline"
        >
          {content.postUrl}
        </a>
      </div>
    );
  }

  return null;
}
