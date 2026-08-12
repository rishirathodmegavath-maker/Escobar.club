import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { contentApi } from "@/api/content";
import { draftsApi } from "@/api/drafts";
import { extractErrorMessage } from "@/api/client";
import { useToast } from "@/components/Toast";
import { Button } from "@/components/Button";
import { TextArea } from "@/components/Field";
import { DraftRestoreBanner } from "@/components/DraftRestoreBanner";
import { useDraftAutosave, useDraftRestore } from "@/hooks/useDraftAutosave";
import { MediaUploadField } from "./MediaUploadField";
import type { Campaign, MediaType } from "@/types";

interface ContentDraft {
  caption: string;
  media: { url: string; mediaType: MediaType } | null;
}

export function ContentSubmitCard({ campaign }: { campaign: Campaign }) {
  const [caption, setCaption] = useState("");
  const [media, setMedia] = useState<{ url: string; mediaType: MediaType } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const queryClient = useQueryClient();
  const { push } = useToast();

  const draftKey = `content-submit-${campaign.id}`;
  const { draft, dismiss: dismissDraft } = useDraftRestore<ContentDraft>(draftKey);
  const hasInput = caption.trim().length > 0 || media !== null;
  const draftSaveStatus = useDraftAutosave(draftKey, { caption, media }, hasInput);

  const mutation = useMutation({
    mutationFn: () => {
      if (!media) throw new Error("Please upload media first");
      return contentApi.submit(campaign.id, { caption, mediaUrl: media.url, mediaType: media.mediaType });
    },
    onSuccess: () => {
      push("Content submitted for review!", "success");
      queryClient.invalidateQueries({ queryKey: ["content", "me"] });
      setCaption("");
      setMedia(null);
      draftsApi.remove(draftKey).catch(() => {});
      dismissDraft();
    },
    onError: (err) => setError(extractErrorMessage(err, "Could not submit content")),
  });

  return (
    <div className="card-surface flex flex-col gap-4 border-signal-200/70 p-6">
      {error && <div className="rounded-lg border border-danger-200 bg-danger-soft px-3 py-2 text-sm text-danger-deep">{error}</div>}

      {draft && (
        <DraftRestoreBanner
          updatedAt={draft.updatedAt}
          onRestore={() => {
            setCaption(draft.data.caption);
            setMedia(draft.data.media);
            dismissDraft();
          }}
          onDiscard={() => {
            draftsApi.remove(draftKey).catch(() => {});
            dismissDraft();
          }}
        />
      )}

      <MediaUploadField value={media} onChange={setMedia} />
      <TextArea
        label="Caption"
        placeholder="Write the caption you'd post alongside this content…"
        value={caption}
        onChange={(e) => setCaption(e.target.value)}
      />
      <div className="flex items-center gap-3">
        <Button
          onClick={() => mutation.mutate()}
          isLoading={mutation.isPending}
          disabled={!media || caption.trim().length === 0}
          className="self-start"
        >
          Submit for review
        </Button>
        {draftSaveStatus === "saving" && <span className="text-xs text-ink-400">Saving draft…</span>}
        {draftSaveStatus === "saved" && <span className="text-xs text-ink-400">Draft saved</span>}
      </div>
    </div>
  );
}
