import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { businessesApi } from "@/api/businesses";
import { extractErrorMessage } from "@/api/client";
import { useToast } from "@/components/Toast";
import { Button } from "@/components/Button";
import { Input, TextArea } from "@/components/Field";
import { FullPageSpinner } from "@/components/Spinner";
import { ImageUploadField } from "@/components/ImageUploadField";

const schema = z.object({
  companyName: z.string().min(2).max(150),
  gstNumber: z.string().min(1, "GST Number is required").max(20),
  contactPersonName: z.string().min(1, "Contact person name is required").max(150),
  mobileNumber: z.string().min(1, "Mobile number is required").max(20),
  industry: z.string().max(80).optional().or(z.literal("")),
  description: z.string().max(4000).optional().or(z.literal("")),
  logoUrl: z.string().optional().or(z.literal("")),
  website: z.string().url("Must be a valid URL").optional().or(z.literal("")),
});
type FormValues = z.infer<typeof schema>;

export function BusinessProfilePage() {
  const { data, isLoading } = useQuery({ queryKey: ["business", "me"], queryFn: businessesApi.getMine });
  const queryClient = useQueryClient();
  const { push } = useToast();

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (data) {
      reset({
        companyName: data.companyName,
        gstNumber: data.gstNumber,
        contactPersonName: data.contactPersonName,
        mobileNumber: data.mobileNumber,
        industry: data.industry ?? "",
        description: data.description ?? "",
        logoUrl: data.logoUrl ?? "",
        website: data.website ?? "",
      });
    }
  }, [data, reset]);

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      businessesApi.updateMine({
        companyName: values.companyName,
        gstNumber: values.gstNumber,
        contactPersonName: values.contactPersonName,
        mobileNumber: values.mobileNumber,
        industry: values.industry ?? "",
        description: values.description ?? "",
        logoUrl: values.logoUrl ?? "",
        website: values.website ?? "",
      }),
    onSuccess: () => {
      push("Company profile updated", "success");
      queryClient.invalidateQueries({ queryKey: ["business", "me"] });
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  const logoUrl = watch("logoUrl");

  if (isLoading) return <FullPageSpinner />;

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="font-display text-3xl font-semibold text-ink-900">Company profile</h1>
      <p className="mt-1.5 mb-6 text-ink-500">This is your public page — creators will see this before applying.</p>

      <form onSubmit={handleSubmit((v) => mutation.mutate(v))} className="card-surface flex flex-col gap-5 p-7">
        <ImageUploadField
          label="Company logo"
          value={logoUrl}
          uploadFn={businessesApi.uploadLogo}
          onChange={(url) => setValue("logoUrl", url, { shouldDirty: true })}
        />
        <Input label="Company Legal Name" error={errors.companyName?.message} {...register("companyName")} />
        <Input label="GST Number" error={errors.gstNumber?.message} {...register("gstNumber")} />
        <Input label="Contact Person Name" error={errors.contactPersonName?.message} {...register("contactPersonName")} />
        <Input label="Mobile Number" type="tel" error={errors.mobileNumber?.message} {...register("mobileNumber")} />
        <Input label="Industry" placeholder="e.g. Apparel, F&B, SaaS" error={errors.industry?.message} {...register("industry")} />
        <Input label="Website" placeholder="https://…" error={errors.website?.message} {...register("website")} />
        <TextArea label="Public description" rows={5} error={errors.description?.message} {...register("description")} />

        <Button type="submit" isLoading={isSubmitting || mutation.isPending} className="self-start">
          Save changes
        </Button>
      </form>
    </div>
  );
}
