import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { kycApi } from "@/api/kyc";
import { extractErrorMessage } from "@/api/client";
import { useToast } from "@/components/Toast";
import { useAuth } from "@/auth/AuthContext";
import { Button } from "@/components/Button";
import { Input } from "@/components/Field";
import { StatusPill } from "@/components/StatusPill";
import { FullPageSpinner } from "@/components/Spinner";
import { KycDocumentUploadField } from "@/features/kyc/KycDocumentUploadField";
import { DraftRestoreBanner } from "@/components/DraftRestoreBanner";
import { useDraftAutosave, useDraftRestore } from "@/hooks/useDraftAutosave";
import { draftsApi } from "@/api/drafts";

const DRAFT_KEY = "creator-kyc";

const schema = z.object({
  panNumber: z
    .string()
    .regex(/^[A-Z]{5}[0-9]{4}[A-Z]$/, "Must be a valid 10-character PAN (e.g. ABCDE1234F)"),
  nameOnPan: z.string().min(2).max(150),
  documentKey: z.string().optional(),
});
type FormValues = z.infer<typeof schema>;

export function CreatorKycPage() {
  const { user } = useAuth();
  const { data, isLoading } = useQuery({
    queryKey: ["kyc", "me"],
    queryFn: kycApi.mine,
    retry: false,
  });
  const queryClient = useQueryClient();
  const { push } = useToast();

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    setError,
    watch,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { documentKey: "" } });

  const { draft, dismiss: dismissDraft } = useDraftRestore<FormValues>(DRAFT_KEY);
  const watchedValues = watch();
  const draftSaveStatus = useDraftAutosave(DRAFT_KEY, watchedValues, !isLoading && isDirty);

  useEffect(() => {
    if (data) {
      reset({
        panNumber: data.panNumberMasked.startsWith("XXXXXX") ? "" : data.panNumberMasked,
        nameOnPan: data.nameOnPan,
        documentKey: "",
      });
    }
  }, [data, reset]);

  const mutation = useMutation({
    mutationFn: (values: FormValues) => kycApi.submit(values),
    onSuccess: () => {
      push("KYC submitted for review", "success");
      queryClient.invalidateQueries({ queryKey: ["kyc", "me"] });
      draftsApi.remove(DRAFT_KEY).catch(() => {});
      dismissDraft();
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  const documentKey = watch("documentKey");

  const onSubmit = (values: FormValues) => {
    if (!values.documentKey && !data?.hasDocument) {
      setError("documentKey", { message: "Upload your PAN card" });
      return;
    }
    mutation.mutate(values);
  };

  if (isLoading) return <FullPageSpinner />;

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="font-display text-3xl font-semibold text-ink-900">KYC verification</h1>
      <p className="mt-1.5 mb-6 text-ink-500">
        Required before you can receive payouts. Submitted once, verified by a brand you work with.
      </p>

      {draft && (
        <DraftRestoreBanner
          updatedAt={draft.updatedAt}
          onRestore={() => {
            reset(draft.data);
            dismissDraft();
          }}
          onDiscard={() => {
            draftsApi.remove(DRAFT_KEY).catch(() => {});
            dismissDraft();
          }}
        />
      )}

      {data && (
        <div className="card-surface mb-6 flex items-center gap-3 p-5">
          <StatusPill status={data.status} />
          <div className="text-sm text-ink-500">
            {data.status === "PENDING" && "Your KYC is awaiting review."}
            {data.status === "VERIFIED" && "Your KYC has been verified — you're eligible for payouts."}
            {data.status === "REJECTED" && (
              <>
                Your KYC was rejected{data.reviewNote ? `: ${data.reviewNote}` : "."} Please resubmit below.
              </>
            )}
          </div>
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="card-surface flex flex-col gap-5 p-7">
        <Input
          label="PAN number"
          placeholder="ABCDE1234F"
          error={errors.panNumber?.message}
          {...register("panNumber", {
            onChange: (e) => (e.target.value = e.target.value.toUpperCase()),
          })}
        />
        <Input label="Name as it appears on the PAN card" error={errors.nameOnPan?.message} {...register("nameOnPan")} />
        <KycDocumentUploadField
          value={documentKey || null}
          onChange={(key) => setValue("documentKey", key, { shouldValidate: true })}
          error={errors.documentKey?.message}
          creatorId={user!.id}
          hasExistingDocument={!!data?.hasDocument}
        />

        <div className="flex items-center gap-3">
          <Button type="submit" isLoading={isSubmitting || mutation.isPending} className="self-start">
            {data ? "Resubmit KYC" : "Submit KYC"}
          </Button>
          {draftSaveStatus === "saving" && <span className="text-xs text-ink-400">Saving draft…</span>}
          {draftSaveStatus === "saved" && <span className="text-xs text-ink-400">Draft saved</span>}
        </div>
      </form>
    </div>
  );
}
