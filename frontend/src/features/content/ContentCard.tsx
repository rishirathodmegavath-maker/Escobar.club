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
import { VideoIcon } from "@/components/icons";

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

  if (!expanded) {
    return (
      <button
        type="button"
        onClick={() => setExpanded(true)}
        className="focus-ring group relative flex aspect-square flex-col overflow-hidden rounded-2xl border border-ink-100 bg-surface-1 text-left shadow-sm transition hover:shadow-md"
      >
        {content.mediaType === "IMAGE" ? (
          <img
            src={content.mediaUrl}
            alt=""
            className="h-full w-full object-cover transition duration-200 group-hover:scale-105"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-signal-500 to-signal-800">
            <VideoIcon className="h-8 w-8 text-white/90" />
          </div>
        )}
        <div className="absolute inset-x-0 top-0 flex items-center gap-2 bg-gradient-to-b from-black/70 via-black/10 to-transparent p-2.5">
          <Avatar name={content.businessCompanyName} imageUrl={content.businessLogoUrl} size={22} />
          <span className="truncate text-xs font-medium text-white">{content.campaignTitle}</span>
        </div>
        <div className="absolute inset-x-0 bottom-0 flex items-center justify-between gap-2 bg-gradient-to-t from-black/70 via-black/10 to-transparent p-2.5">
          <StatusPill status={content.status} />
        </div>
      </button>
    );
  }

  return (
    <div className="card-surface col-span-full mx-auto flex w-full max-w-2xl flex-col gap-4 p-6">
      <button
        onClick={() => setExpanded(false)}
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
    </div>
  );
}
