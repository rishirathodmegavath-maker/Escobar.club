import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import clsx from "clsx";
import type { ContentRecord, MediaType } from "@/types";
import { StatusPill } from "@/components/StatusPill";
import { Button } from "@/components/Button";
import { TextArea } from "@/components/Field";
import { MediaUploadField } from "./MediaUploadField";
import { ReviewNotesTimeline } from "./ReviewNotesTimeline";
import { PublishContentForm } from "./PublishContentForm";
import { PublishedLinkStatus } from "./PublishedLinkStatus";
import { ContentMetricsPanel } from "./ContentMetricsPanel";
import { PayoutPanel } from "./PayoutPanel";
import { contentApi } from "@/api/content";
import { extractErrorMessage } from "@/api/client";
import { useToast } from "@/components/Toast";
import { Avatar } from "@/components/Avatar";

export function ContentCard({ content }: { content: ContentRecord }) {
  const [expanded, setExpanded] = useState(false);
  const [editing, setEditing] = useState(false);
  const [caption, setCaption] = useState(content.caption ?? "");
  const [media, setMedia] = useState<{ url: string; mediaType: MediaType } | null>({
    url: content.mediaUrl,
    mediaType: content.mediaType,
  });
  const [error, setError] = useState<string | null>(null);
  const queryClient = useQueryClient();
  const { push } = useToast();

  const mutation = useMutation({
    mutationFn: () => {
      if (!media) throw new Error("Please upload media first");
      return contentApi.resubmit(content.id, { caption, mediaUrl: media.url, mediaType: media.mediaType });
    },
    onSuccess: () => {
      push("Resubmitted for review!", "success");
      queryClient.invalidateQueries({ queryKey: ["content", "me"] });
      setEditing(false);
    },
    onError: (err) => setError(extractErrorMessage(err, "Could not resubmit content")),
  });

  return (
    <div className="card-surface flex flex-col gap-4 p-6">
      <button
        onClick={() => setExpanded((v) => !v)}
        className="focus-ring flex items-start justify-between gap-4 text-left"
      >
        <div className="flex items-center gap-3">
          <Avatar name={content.businessCompanyName} imageUrl={content.businessLogoUrl} size={40} />
          <div>
            <span className="flex items-center gap-2 font-display text-lg font-semibold text-ink-900 hover:text-signal-700">
              {content.campaignTitle}
              <span className={clsx("text-xs text-ink-300 transition-transform", expanded && "rotate-180")}>▾</span>
            </span>
            <p className="text-xs text-ink-400">
              {content.businessCompanyName} · Version {content.version}
            </p>
          </div>
        </div>
        <StatusPill status={content.status} />
      </button>

      {expanded && (
        <>
          {!editing && (
            <>
              {content.mediaType === "IMAGE" ? (
                <img src={content.mediaUrl} alt="" className="max-h-64 w-full rounded-lg object-cover" />
              ) : (
                <video src={content.mediaUrl} controls className="max-h-64 w-full rounded-lg" />
              )}
              {content.caption && <p className="text-sm text-ink-600">{content.caption}</p>}
            </>
          )}

          {content.status === "CHANGES_REQUESTED" && (
            <div className="border-t border-ink-100 pt-4">
              {editing ? (
                <div className="flex flex-col gap-4">
                  {error && (
                    <div className="rounded-lg border border-danger-200 bg-danger-soft px-3 py-2 text-sm text-danger-deep">
                      {error}
                    </div>
                  )}
                  <MediaUploadField value={media} onChange={setMedia} />
                  <TextArea label="Caption" value={caption} onChange={(e) => setCaption(e.target.value)} />
                  <div className="flex gap-2">
                    <Button isLoading={mutation.isPending} onClick={() => mutation.mutate()}>
                      Resubmit for review
                    </Button>
                    <Button variant="ghost" onClick={() => setEditing(false)}>
                      Cancel
                    </Button>
                  </div>
                </div>
              ) : (
                <Button variant="gold" onClick={() => setEditing(true)}>
                  Edit &amp; resubmit
                </Button>
              )}
            </div>
          )}

          {content.status === "APPROVED" && <PublishContentForm content={content} />}

          <PublishedLinkStatus content={content} />

          {content.status === "PUBLISHED" && (
            <>
              <ContentMetricsPanel content={content} />
              <PayoutPanel content={content} />
            </>
          )}

          <div className="border-t border-ink-100 pt-4">
            <ReviewNotesTimeline notes={content.reviewNotes} />
          </div>
        </>
      )}
    </div>
  );
}
