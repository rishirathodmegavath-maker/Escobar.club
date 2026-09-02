import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import type { ContentRecord } from "@/types";
import { StatusPill } from "@/components/StatusPill";
import { Button } from "@/components/Button";
import { TextArea } from "@/components/Field";
import { ReviewNotesTimeline } from "./ReviewNotesTimeline";
import { PublishedLinkStatus } from "./PublishedLinkStatus";
import { ContentMetricsPanel } from "./ContentMetricsPanel";
import { PayoutPanel } from "./PayoutPanel";
import { contentApi } from "@/api/content";
import { extractErrorMessage } from "@/api/client";
import { useToast } from "@/components/Toast";
import { Avatar } from "@/components/Avatar";
import { ChevronRightIcon } from "@/components/icons";

type ReviewDecision = "APPROVED" | "REJECTED" | "CHANGES_REQUESTED";

const decisionCopy: Record<ReviewDecision, string> = {
  APPROVED: "approved",
  REJECTED: "rejected",
  CHANGES_REQUESTED: "sent back for changes",
};

export function ContentReviewCard({ content }: { content: ContentRecord }) {
  const [decision, setDecision] = useState<ReviewDecision | null>(null);
  const [note, setNote] = useState("");
  const queryClient = useQueryClient();
  const { push } = useToast();

  const mutation = useMutation({
    mutationFn: (d: "APPROVED" | "REJECTED" | "CHANGES_REQUESTED") =>
      contentApi.review(content.id, { decision: d, note: note || undefined }),
    onSuccess: (_, d) => {
      push(`Content ${decisionCopy[d]}.`, "success");
      queryClient.invalidateQueries({ queryKey: ["content", "review-queue"] });
      setDecision(null);
      setNote("");
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  return (
    <div className="card-surface flex flex-col gap-4 p-6">
      <div className="flex items-start justify-between gap-4">
        <Link to={`/business/creators/${content.creatorId}`} className="focus-ring flex items-center gap-3">
          <Avatar name={content.creatorDisplayName} imageUrl={content.creatorProfilePictureUrl} size={40} />
          <div>
            <span className="flex items-center gap-1.5 font-display text-lg font-semibold text-ink-900 hover:text-signal-700">
              {content.creatorDisplayName}
              <ChevronRightIcon className="h-4 w-4 text-ink-300" />
            </span>
            <p className="text-xs text-ink-400">Version {content.version}</p>
          </div>
        </Link>
        <StatusPill status={content.status} />
      </div>

      {content.mediaType === "IMAGE" ? (
        <img src={content.mediaUrl} alt="" className="max-h-72 w-full rounded-lg object-cover" />
      ) : (
        <video src={content.mediaUrl} controls className="max-h-72 w-full rounded-lg" />
      )}
      {content.caption && <p className="text-sm text-ink-600">{content.caption}</p>}

      {content.status === "SUBMITTED" && (
        <div className="border-t border-ink-100 pt-4">
          {decision ? (
            <div className="flex flex-col gap-3">
              <TextArea
                label="Note (optional)"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                rows={3}
                placeholder={decision === "CHANGES_REQUESTED" ? "Explain what needs to change…" : "Add a comment…"}
              />
              <div className="flex flex-wrap gap-2">
                <Button
                  isLoading={mutation.isPending}
                  onClick={() => mutation.mutate(decision)}
                  variant={decision === "APPROVED" ? "primary" : decision === "REJECTED" ? "danger" : "gold"}
                >
                  Confirm {decisionCopy[decision]}
                </Button>
                <Button variant="ghost" onClick={() => setDecision(null)}>
                  Cancel
                </Button>
              </div>
            </div>
          ) : (
            <div className="flex flex-wrap gap-2">
              <Button onClick={() => setDecision("APPROVED")}>Approve</Button>
              <Button variant="gold" onClick={() => setDecision("CHANGES_REQUESTED")}>
                Request changes
              </Button>
              <Button variant="danger" onClick={() => setDecision("REJECTED")}>
                Reject
              </Button>
            </div>
          )}
        </div>
      )}

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
