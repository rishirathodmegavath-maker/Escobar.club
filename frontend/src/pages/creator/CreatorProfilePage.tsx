import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useFieldArray, useForm, type FieldErrors } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { creatorsApi } from "@/api/creators";
import { extractErrorMessage } from "@/api/client";
import { useToast } from "@/components/Toast";
import { Button } from "@/components/Button";
import { Input, TextArea } from "@/components/Field";
import { FullPageSpinner } from "@/components/Spinner";
import { TickMeter } from "@/components/TickMeter";
import { ImageUploadField } from "@/components/ImageUploadField";
import { DraftRestoreBanner } from "@/components/DraftRestoreBanner";
import { useDraftAutosave, useDraftRestore } from "@/hooks/useDraftAutosave";
import { draftsApi } from "@/api/drafts";
import { isUrlLike, normalizeUrl } from "@/lib/url";

const DRAFT_KEY = "creator-profile";

// A link typed without a scheme (e.g. "instagram.com/handle") is normalized to https:// rather
// than rejected - see @/lib/url. The real boundary is still the backend's identical http(s)-only
// @Pattern on CreatorProfileUpdateRequest.portfolioLinks, which the normalized value always satisfies.
const linkSchema = z.object({
  value: z
    .string()
    .transform((v) => normalizeUrl(v))
    .refine((v) => isUrlLike(v), "Enter a valid URL"),
});
const INSTAGRAM_PROFILE_PATTERN = /^https?:\/\/(www\.)?instagram\.com\/.+/;
const schema = z.object({
  displayName: z.string().min(2).max(120),
  bio: z.string().max(4000).optional().or(z.literal("")),
  profilePictureUrl: z.string().optional().or(z.literal("")),
  niche: z.string().max(80).optional().or(z.literal("")),
  openToOtherNiches: z.enum(["true", "false"]),
  instagramProfileUrl: z
    .string()
    .transform((v) => normalizeUrl(v))
    .refine((v) => isUrlLike(v) && INSTAGRAM_PROFILE_PATTERN.test(v), "Enter a valid Instagram profile URL"),
  followerCount: z.coerce.number().int().min(0),
  portfolioLinks: z.array(linkSchema).max(30),
});
type FormValues = z.infer<typeof schema>;

export function CreatorProfilePage() {
  const { data, isLoading } = useQuery({ queryKey: ["creator", "me"], queryFn: creatorsApi.getMine });
  const queryClient = useQueryClient();
  const { push } = useToast();
  const navigate = useNavigate();

  const {
    register,
    control,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      displayName: "",
      bio: "",
      profilePictureUrl: "",
      niche: "",
      openToOtherNiches: "false",
      instagramProfileUrl: "",
      followerCount: 0,
      portfolioLinks: [],
    },
  });

  const portfolioFields = useFieldArray({ control, name: "portfolioLinks" });
  const followerCount = watch("followerCount");
  const profilePictureUrl = watch("profilePictureUrl");

  const { draft, dismiss: dismissDraft } = useDraftRestore<FormValues>(DRAFT_KEY);
  const watchedValues = watch();
  const draftSaveStatus = useDraftAutosave(DRAFT_KEY, watchedValues, !isLoading && isDirty);

  useEffect(() => {
    if (data) {
      reset({
        displayName: data.displayName,
        bio: data.bio ?? "",
        profilePictureUrl: data.profilePictureUrl ?? "",
        niche: data.niche ?? "",
        openToOtherNiches: data.openToOtherNiches ? "true" : "false",
        instagramProfileUrl: data.instagramProfileUrl,
        followerCount: data.followerCount,
        portfolioLinks: data.portfolioLinks.map((v) => ({ value: v })),
      });
    }
  }, [data, reset]);

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      creatorsApi.updateMine({
        displayName: values.displayName,
        bio: values.bio ?? "",
        profilePictureUrl: values.profilePictureUrl ?? "",
        niche: values.niche ?? "",
        openToOtherNiches: values.openToOtherNiches === "true",
        instagramProfileUrl: values.instagramProfileUrl,
        followerCount: values.followerCount,
        portfolioLinks: values.portfolioLinks.map((l) => l.value),
      }),
    onSuccess: (updated) => {
      push("Profile updated", "success");
      queryClient.setQueryData(["creator", "me"], updated);
      queryClient.invalidateQueries({ queryKey: ["creator"] });
      draftsApi.remove(DRAFT_KEY).catch(() => {});
      dismissDraft();
      navigate("/creator/profile");
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  if (isLoading) return <FullPageSpinner />;

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="font-display text-3xl font-semibold text-ink-900">My profile</h1>
      <p className="mt-1.5 mb-6 text-ink-500">This is what businesses see when reviewing your content submissions.</p>

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

      <form onSubmit={handleSubmit((v) => mutation.mutate(v))} className="card-surface flex flex-col gap-5 p-7">
        <ImageUploadField
          label="Profile picture"
          value={profilePictureUrl}
          uploadFn={creatorsApi.uploadProfilePicture}
          onChange={(url) => setValue("profilePictureUrl", url, { shouldDirty: true })}
          shape="circle"
        />
        <Input label="Display name" error={errors.displayName?.message} {...register("displayName")} />
        <Input
          label="Instagram profile link"
          placeholder="https://instagram.com/yourhandle"
          error={errors.instagramProfileUrl?.message}
          {...register("instagramProfileUrl")}
        />
        <Input label="Niche / category" placeholder="e.g. Beauty, Fitness, Tech" error={errors.niche?.message} {...register("niche")} />
        <label className="flex flex-col gap-1.5">
          <span className="text-sm font-medium text-ink-700">Open to other niches?</span>
          <select
            className="focus-ring w-full rounded-lg border border-ink-200 bg-surface-input px-3.5 py-2.5 text-sm text-ink-900"
            {...register("openToOtherNiches")}
          >
            <option value="false">No — only my primary niche</option>
            <option value="true">Yes — consider me for other niches too</option>
          </select>
        </label>
        <TextArea label="Bio" rows={4} error={errors.bio?.message} {...register("bio")} />

        <div>
          <Input
            label="Follower count (self-reported)"
            type="number"
            min={0}
            error={errors.followerCount?.message}
            {...register("followerCount")}
          />
          <TickMeter value={Number(followerCount) || 0} max={100000} className="mt-3" accent="gold" />
        </div>

        <FieldArraySection
          title="Portfolio links"
          fields={portfolioFields.fields}
          onAdd={() => portfolioFields.append({ value: "" })}
          onRemove={portfolioFields.remove}
          register={register}
          name="portfolioLinks"
          errors={errors.portfolioLinks}
        />

        <div className="flex items-center gap-3">
          <Button type="submit" isLoading={isSubmitting || mutation.isPending} className="self-start">
            Save changes
          </Button>
          {draftSaveStatus === "saving" && <span className="text-xs text-ink-400">Saving draft…</span>}
          {draftSaveStatus === "saved" && <span className="text-xs text-ink-400">Draft saved</span>}
        </div>
      </form>
    </div>
  );
}

function FieldArraySection({
  title,
  fields,
  onAdd,
  onRemove,
  register,
  name,
  errors,
}: {
  title: string;
  fields: { id: string }[];
  onAdd: () => void;
  onRemove: (index: number) => void;
  register: ReturnType<typeof useForm<FormValues>>["register"];
  name: "portfolioLinks";
  errors?: FieldErrors<FormValues>["portfolioLinks"];
}) {
  return (
    <div>
      <div className="mb-2 flex items-center justify-between">
        <span className="text-sm font-medium text-ink-700">{title}</span>
        <button type="button" onClick={onAdd} className="focus-ring text-xs font-semibold text-signal-600 hover:text-signal-700">
          + Add link
        </button>
      </div>
      <div className="flex flex-col gap-2">
        {fields.length === 0 && <p className="text-xs text-ink-400">No links added yet.</p>}
        {fields.map((field, index) => (
          <div key={field.id} className="flex gap-2">
            <Input
              placeholder="https://…"
              error={errors?.[index]?.value?.message}
              {...register(`${name}.${index}.value` as const)}
            />
            <button
              type="button"
              onClick={() => onRemove(index)}
              className="focus-ring shrink-0 rounded-lg px-2 text-sm text-ink-400 hover:bg-danger-soft hover:text-danger-deep"
            >
              Remove
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
