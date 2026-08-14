import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { campaignsApi } from "@/api/campaigns";
import { extractErrorMessage } from "@/api/client";
import { useToast } from "@/components/Toast";
import { Button } from "@/components/Button";
import { Input } from "@/components/Field";
import type { Campaign } from "@/types";

const dateFormatter = new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short", year: "numeric" });

function formatDate(iso: string): string {
  return dateFormatter.format(new Date(iso));
}

function daysBetween(fromIso: string, toIso: string): number {
  const from = new Date(fromIso);
  const to = new Date(toIso);
  from.setHours(0, 0, 0, 0);
  to.setHours(0, 0, 0, 0);
  return Math.round((to.getTime() - from.getTime()) / 86_400_000);
}

interface ScheduleFields {
  submissionOpenAt: string;
  submissionDeadline: string;
  publishStartAt: string;
  publishEndAt: string;
}

function validate(fields: ScheduleFields): string | null {
  if (!fields.submissionOpenAt || !fields.submissionDeadline || !fields.publishStartAt || !fields.publishEndAt) {
    return "All four dates are required";
  }
  if (fields.submissionDeadline < fields.submissionOpenAt) {
    return "Submission deadline must be on or after submissions open";
  }
  if (fields.publishStartAt < fields.submissionDeadline) {
    return "Publishing can't start before the submission deadline";
  }
  if (fields.publishEndAt < fields.publishStartAt) {
    return "Publish end date must be on or after the publish start date";
  }
  const today = new Date().toISOString().slice(0, 10);
  if (fields.publishStartAt <= today) {
    return "The new publishing start date must be in the future";
  }
  return null;
}

function ScheduleRow({ label, current, next }: { label: string; current: string; next: string }) {
  const changed = current !== next;
  return (
    <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-3 py-1.5 text-sm">
      <span className="text-ink-500">{label}</span>
      <span className={changed ? "font-mono text-ink-400 line-through" : "font-mono text-ink-400"}>
        {formatDate(current)}
      </span>
      <span className={changed ? "font-mono font-semibold text-signal-600" : "font-mono text-ink-600"}>
        {formatDate(next)}
      </span>
    </div>
  );
}

export function ChangeScheduleDialog({ campaign, onClose }: { campaign: Campaign; onClose: () => void }) {
  const [stage, setStage] = useState<"edit" | "confirm">("edit");
  const [fields, setFields] = useState<ScheduleFields>({
    submissionOpenAt: campaign.submissionOpenAt,
    submissionDeadline: campaign.submissionDeadline,
    publishStartAt: campaign.publishStartAt,
    publishEndAt: campaign.publishEndAt,
  });
  const [error, setError] = useState<string | null>(null);
  const queryClient = useQueryClient();
  const { push } = useToast();

  const mutation = useMutation({
    mutationFn: () => campaignsApi.updateSchedule(campaign.id, fields),
    onSuccess: () => {
      push("Campaign schedule updated", "success");
      queryClient.invalidateQueries({ queryKey: ["campaigns", "mine"] });
      queryClient.invalidateQueries({ queryKey: ["campaign", campaign.id] });
      onClose();
    },
    onError: (err) => setError(extractErrorMessage(err, "Could not update the campaign schedule")),
  });

  const handleReview = () => {
    const validationError = validate(fields);
    if (validationError) {
      setError(validationError);
      return;
    }
    setError(null);
    setStage("confirm");
  };

  const direction =
    fields.publishStartAt === campaign.publishStartAt
      ? "unchanged"
      : fields.publishStartAt < campaign.publishStartAt
        ? "prepone"
        : "postpone";
  const shiftDays = Math.abs(daysBetween(campaign.publishStartAt, fields.publishStartAt));

  return (
    <div className="flex flex-col gap-4 rounded-lg border border-signal-200/70 bg-surface p-5">
      <div>
        <h4 className="font-display text-base font-semibold text-ink-900">📅 Change Schedule</h4>
        <p className="text-xs text-ink-400">Prepone or postpone this campaign's submission and publishing dates.</p>
      </div>

      {error && <div className="rounded-lg border border-danger-200 bg-danger-soft px-3 py-2 text-sm text-danger-deep">{error}</div>}

      {stage === "edit" ? (
        <>
          <div className="grid grid-cols-2 gap-4">
            <Input
              type="date"
              label="Submission opens"
              value={fields.submissionOpenAt}
              onChange={(e) => setFields((f) => ({ ...f, submissionOpenAt: e.target.value }))}
            />
            <Input
              type="date"
              label="Submission deadline"
              value={fields.submissionDeadline}
              onChange={(e) => setFields((f) => ({ ...f, submissionDeadline: e.target.value }))}
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input
              type="date"
              label="Publishing start date"
              value={fields.publishStartAt}
              onChange={(e) => setFields((f) => ({ ...f, publishStartAt: e.target.value }))}
            />
            <Input
              type="date"
              label="Publishing end date"
              value={fields.publishEndAt}
              onChange={(e) => setFields((f) => ({ ...f, publishEndAt: e.target.value }))}
            />
          </div>
          <div className="flex items-center gap-2">
            <Button size="sm" onClick={handleReview}>
              Review changes
            </Button>
            <Button variant="ghost" size="sm" onClick={onClose}>
              Cancel
            </Button>
          </div>
        </>
      ) : (
        <>
          {direction !== "unchanged" && (
            <div
              className={
                direction === "prepone"
                  ? "w-fit rounded-full bg-signal-100 px-3 py-1 font-mono text-xs font-semibold uppercase tracking-wide text-signal-700"
                  : "w-fit rounded-full bg-gold-soft px-3 py-1 font-mono text-xs font-semibold uppercase tracking-wide text-gold-deep"
              }
            >
              {direction === "prepone" ? "⏪ Preponed" : "⏩ Postponed"} by {shiftDays} day{shiftDays === 1 ? "" : "s"}
            </div>
          )}

          <div className="rounded-lg border border-ink-100 p-4">
            <div className="grid grid-cols-[1fr_auto_1fr] gap-3 pb-2 text-xs font-semibold uppercase tracking-wide text-ink-400">
              <span>Field</span>
              <span>Current Schedule</span>
              <span>New Schedule</span>
            </div>
            <ScheduleRow label="Submission opens" current={campaign.submissionOpenAt} next={fields.submissionOpenAt} />
            <ScheduleRow label="Submission deadline" current={campaign.submissionDeadline} next={fields.submissionDeadline} />
            <ScheduleRow label="Publishing starts" current={campaign.publishStartAt} next={fields.publishStartAt} />
            <ScheduleRow label="Publishing ends" current={campaign.publishEndAt} next={fields.publishEndAt} />
          </div>

          <p className="text-xs text-ink-400">
            Already-submitted content and its review history won't be affected — only the campaign's submission and
            publishing windows change.
          </p>

          <div className="flex items-center gap-2">
            <Button size="sm" isLoading={mutation.isPending} onClick={() => mutation.mutate()}>
              Confirm & save
            </Button>
            <Button variant="ghost" size="sm" onClick={() => setStage("edit")} disabled={mutation.isPending}>
              Back to edit
            </Button>
          </div>
        </>
      )}
    </div>
  );
}
