import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import clsx from "clsx";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { campaignsApi } from "@/api/campaigns";
import { extractErrorMessage } from "@/api/client";
import { useToast } from "@/components/Toast";
import { Button } from "@/components/Button";
import { Input, TextArea } from "@/components/Field";
import { StatusPill } from "@/components/StatusPill";
import { FullPageSpinner } from "@/components/Spinner";
import { EmptyState } from "@/components/EmptyState";
import { CompassIcon } from "@/components/icons";
import { Pagination } from "@/components/Pagination";
import { ChangeScheduleDialog } from "@/features/campaigns/ChangeScheduleDialog";
import type { Campaign, ManualCampaignStatus } from "@/types";

type StatFilter = "LIVE" | "PENDING";

const schema = z
  .object({
    title: z.string().min(2, "Give your campaign a name").max(150),
    description: z.string().max(4000).optional().or(z.literal("")),
    submissionOpenAt: z.string().min(1, "Submission open date is required"),
    submissionDeadline: z.string().min(1, "Submission deadline is required"),
    publishStartAt: z.string().min(1, "Publish start date is required"),
    publishEndAt: z.string().min(1, "Publish end date is required"),
    ratePerThousandViewsInr: z.coerce.number().min(100, "Rate must be at least ₹100 per 1,000 views"),
    status: z.enum(["DRAFT", "PUBLISHED", "CANCELLED"]).optional(),
    urgent: z.boolean().optional(),
  })
  .refine((data) => data.submissionDeadline >= data.submissionOpenAt, {
    message: "Submission deadline must be on or after submissions open",
    path: ["submissionDeadline"],
  })
  .refine((data) => data.publishStartAt >= data.submissionDeadline, {
    message: "Publishing can't start before the submission deadline",
    path: ["publishStartAt"],
  })
  .refine((data) => data.publishEndAt >= data.publishStartAt, {
    message: "Publish end date must be on or after the publish start date",
    path: ["publishEndAt"],
  });
type FormValues = z.infer<typeof schema>;

const inrFormatter = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });

function CampaignForm({
  defaultValues,
  showStatus,
  submitLabel,
  onSubmit,
  isPending,
}: {
  defaultValues?: Partial<FormValues>;
  showStatus: boolean;
  submitLabel: string;
  onSubmit: (values: FormValues) => void;
  isPending: boolean;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
      <Input label="Campaign title" error={errors.title?.message} {...register("title")} />
      <TextArea label="Description" rows={4} error={errors.description?.message} {...register("description")} />
      <div className="grid grid-cols-2 gap-4">
        <Input
          type="date"
          label="Submission opens"
          error={errors.submissionOpenAt?.message}
          {...register("submissionOpenAt")}
        />
        <Input
          type="date"
          label="Submission deadline"
          error={errors.submissionDeadline?.message}
          {...register("submissionDeadline")}
        />
      </div>
      <div className="grid grid-cols-2 gap-4">
        <Input type="date" label="Publishing start date" error={errors.publishStartAt?.message} {...register("publishStartAt")} />
        <Input type="date" label="Publishing end date" error={errors.publishEndAt?.message} {...register("publishEndAt")} />
      </div>
      <Input
        type="number"
        step="0.01"
        min={100}
        label="Rate per 1,000 views (INR)"
        hint="Minimum ₹100 per 1,000 views"
        error={errors.ratePerThousandViewsInr?.message}
        {...register("ratePerThousandViewsInr")}
      />
      <label className="flex items-center gap-2 text-sm font-medium text-ink-700">
        <input type="checkbox" className="focus-ring h-4 w-4 rounded border-ink-300" {...register("urgent")} />
        Mark as urgent (needs creators immediately — shows in the Hot tab on Discover)
      </label>
      {showStatus && (
        <label className="flex flex-col gap-1.5">
          <span className="text-sm font-medium text-ink-700">Status</span>
          <select
            className="focus-ring w-full rounded-lg border border-ink-200 bg-surface-input px-3.5 py-2.5 text-sm text-ink-900"
            {...register("status")}
          >
            <option value="PUBLISHED">Auto (Upcoming / Live / Completed, computed from dates)</option>
            <option value="DRAFT">Draft (hidden from creators)</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </label>
      )}
      <Button type="submit" isLoading={isPending} className="self-start">
        {submitLabel}
      </Button>
    </form>
  );
}

function CampaignListItem({ campaign }: { campaign: Campaign }) {
  const [editing, setEditing] = useState(false);
  const [changingSchedule, setChangingSchedule] = useState(false);
  const queryClient = useQueryClient();
  const { push } = useToast();

  const updateMutation = useMutation({
    mutationFn: (values: FormValues) =>
      campaignsApi.update(campaign.id, {
        title: values.title,
        description: values.description ?? "",
        submissionOpenAt: values.submissionOpenAt,
        submissionDeadline: values.submissionDeadline,
        publishStartAt: values.publishStartAt,
        publishEndAt: values.publishEndAt,
        ratePerThousandViewsInr: values.ratePerThousandViewsInr,
        status: (values.status ?? "PUBLISHED") as ManualCampaignStatus,
        urgent: values.urgent ?? false,
      }),
    onSuccess: () => {
      push("Campaign updated", "success");
      queryClient.invalidateQueries({ queryKey: ["campaigns", "mine"] });
      setEditing(false);
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  const deleteMutation = useMutation({
    mutationFn: () => campaignsApi.remove(campaign.id),
    onSuccess: () => {
      push("Campaign deleted", "success");
      queryClient.invalidateQueries({ queryKey: ["campaigns", "mine"] });
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  return (
    <div className="card-surface flex flex-col gap-3 p-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h3 className="font-display text-base font-semibold text-ink-900">{campaign.title}</h3>
          <p className="mt-0.5 font-mono text-xs text-ink-400">
            Submissions {campaign.submissionOpenAt} – {campaign.submissionDeadline} · Live {campaign.publishStartAt} –{" "}
            {campaign.publishEndAt} · {inrFormatter.format(campaign.ratePerThousandViewsInr)} / 1,000 views
          </p>
        </div>
        <div className="flex shrink-0 flex-col items-end gap-1.5">
          {campaign.hot && (
            <span className="rounded-full bg-alert-soft px-2.5 py-1 font-mono text-xs font-semibold uppercase tracking-wide text-alert-deep">
              🔥 Hot
            </span>
          )}
          <StatusPill status={campaign.status} />
        </div>
      </div>

      {editing ? (
        <div className="border-t border-ink-100 pt-4">
          <CampaignForm
            showStatus
            submitLabel="Save changes"
            isPending={updateMutation.isPending}
            defaultValues={{
              title: campaign.title,
              description: campaign.description ?? "",
              submissionOpenAt: campaign.submissionOpenAt,
              submissionDeadline: campaign.submissionDeadline,
              publishStartAt: campaign.publishStartAt,
              publishEndAt: campaign.publishEndAt,
              ratePerThousandViewsInr: campaign.ratePerThousandViewsInr,
              status: campaign.status === "DRAFT" || campaign.status === "CANCELLED" ? campaign.status : "PUBLISHED",
              urgent: campaign.urgent,
            }}
            onSubmit={(values) => updateMutation.mutate(values)}
          />
          <Button variant="ghost" size="sm" className="mt-2" onClick={() => setEditing(false)}>
            Cancel
          </Button>
        </div>
      ) : changingSchedule ? (
        <div className="border-t border-ink-100 pt-4">
          <ChangeScheduleDialog campaign={campaign} onClose={() => setChangingSchedule(false)} />
        </div>
      ) : (
        <div className="flex items-center gap-2">
          <Button variant="secondary" size="sm" onClick={() => setEditing(true)}>
            Edit
          </Button>
          {campaign.canChangeSchedule && (
            <Button variant="secondary" size="sm" onClick={() => setChangingSchedule(true)}>
              📅 Change Schedule
            </Button>
          )}
          <Button
            variant="danger"
            size="sm"
            isLoading={deleteMutation.isPending}
            onClick={() => {
              if (window.confirm(`Delete "${campaign.title}"? This can't be undone.`)) {
                deleteMutation.mutate();
              }
            }}
          >
            Delete
          </Button>
        </div>
      )}
    </div>
  );
}

export function BusinessCampaignsPage() {
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const [filter, setFilter] = useState<StatFilter | null>(null);
  const queryClient = useQueryClient();
  const { push } = useToast();

  const { data, isLoading } = useQuery({
    queryKey: ["campaigns", "mine", page],
    queryFn: () => campaignsApi.mine(page),
  });

  const toggleFilter = (next: StatFilter) => setFilter((current) => (current === next ? null : next));
  const visibleCampaigns = data
    ? filter === "LIVE"
      ? data.content.filter((c) => c.status === "LIVE")
      : filter === "PENDING"
        ? data.content.filter((c) => c.approvalStatus === "PENDING")
        : data.content
    : [];

  const createMutation = useMutation({
    mutationFn: (values: FormValues) =>
      campaignsApi.create({
        title: values.title,
        description: values.description ?? "",
        submissionOpenAt: values.submissionOpenAt,
        submissionDeadline: values.submissionDeadline,
        publishStartAt: values.publishStartAt,
        publishEndAt: values.publishEndAt,
        ratePerThousandViewsInr: values.ratePerThousandViewsInr,
        urgent: values.urgent ?? false,
      }),
    onSuccess: () => {
      push("Campaign created", "success");
      queryClient.invalidateQueries({ queryKey: ["campaigns", "mine"] });
      setCreating(false);
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl font-semibold text-ink-900">My campaigns</h1>
          <p className="mt-1.5 text-ink-500">Create and manage the campaigns creators can apply to.</p>
        </div>
        {!creating && <Button onClick={() => setCreating(true)}>New campaign</Button>}
      </div>

      {creating && (
        <div className="card-surface p-7">
          <h2 className="mb-4 font-display text-lg font-semibold text-ink-800">New campaign</h2>
          <CampaignForm
            showStatus={false}
            submitLabel="Create campaign"
            isPending={createMutation.isPending}
            onSubmit={(values) => createMutation.mutate(values)}
          />
          <Button variant="ghost" size="sm" className="mt-2" onClick={() => setCreating(false)}>
            Cancel
          </Button>
        </div>
      )}

      {isLoading ? (
        <FullPageSpinner />
      ) : !data || data.content.length === 0 ? (
        <EmptyState
          icon={<CompassIcon className="h-10 w-10" />}
          title="No campaigns yet"
          description="Create your first campaign to start receiving creator content submissions."
        />
      ) : (
        <>
          <div className="grid grid-cols-3 gap-3.5">
            <button
              type="button"
              onClick={() => setFilter(null)}
              className={clsx(
                "card-surface focus-ring flex flex-col gap-1 p-4 text-left transition-colors",
                filter === null ? "ring-2 ring-signal-500" : "hover:bg-surface-hover",
              )}
            >
              <p className="text-[11px] font-semibold uppercase tracking-wide text-ink-400">Total campaigns</p>
              <p className="font-mono text-xl font-bold text-ink-900">{data.totalElements}</p>
            </button>
            <button
              type="button"
              onClick={() => toggleFilter("LIVE")}
              className={clsx(
                "card-surface focus-ring flex flex-col gap-1 p-4 text-left transition-colors",
                filter === "LIVE" ? "ring-2 ring-signal-500" : "hover:bg-surface-hover",
              )}
            >
              <p className="text-[11px] font-semibold uppercase tracking-wide text-ink-400">Live now</p>
              <p className="font-mono text-xl font-bold text-ink-900">
                {data.content.filter((c) => c.status === "LIVE").length}
              </p>
            </button>
            <button
              type="button"
              onClick={() => toggleFilter("PENDING")}
              className={clsx(
                "card-surface focus-ring flex flex-col gap-1 p-4 text-left transition-colors",
                filter === "PENDING" ? "ring-2 ring-signal-500" : "hover:bg-surface-hover",
              )}
            >
              <p className="text-[11px] font-semibold uppercase tracking-wide text-ink-400">Awaiting admin</p>
              <p className="font-mono text-xl font-bold text-ink-900">
                {data.content.filter((c) => c.approvalStatus === "PENDING").length}
              </p>
            </button>
          </div>

          {visibleCampaigns.length === 0 ? (
            <EmptyState icon={<CompassIcon className="h-10 w-10" />} title="Nothing matches this filter" />
          ) : (
            <div className="grid grid-cols-1 items-start gap-4 lg:grid-cols-2">
              {visibleCampaigns.map((campaign) => (
                <CampaignListItem key={campaign.id} campaign={campaign} />
              ))}
            </div>
          )}
        </>
      )}

      {data && <Pagination page={data.page} totalPages={data.totalPages} last={data.last} onPageChange={setPage} />}
    </div>
  );
}
