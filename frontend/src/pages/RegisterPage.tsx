import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link, useNavigate } from "react-router-dom";
import clsx from "clsx";
import { useAuth } from "@/auth/AuthContext";
import { Button } from "@/components/Button";
import { Input } from "@/components/Field";
import { extractErrorMessage } from "@/api/client";
import type { UserRole } from "@/types";

const schema = z
  .object({
    role: z.enum(["CREATOR", "BUSINESS"]),
    displayName: z.string().min(2, "Must be at least 2 characters").max(150),
    email: z.string().email("Enter a valid email address"),
    password: z.string().min(8, "Must be at least 8 characters"),
    gstNumber: z.string().max(20).optional().or(z.literal("")),
    contactPersonName: z.string().max(150).optional().or(z.literal("")),
    mobileNumber: z.string().max(20).optional().or(z.literal("")),
  })
  .superRefine((data, ctx) => {
    if (data.role !== "BUSINESS") return;
    if (!data.gstNumber?.trim()) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: "GST Number is required", path: ["gstNumber"] });
    }
    if (!data.contactPersonName?.trim()) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: "Contact person name is required", path: ["contactPersonName"] });
    }
    if (!data.mobileNumber?.trim()) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: "Mobile number is required", path: ["mobileNumber"] });
    }
  });
type FormValues = z.infer<typeof schema>;

export function RegisterPage() {
  const { register: registerAccount } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);
  const [role, setRole] = useState<UserRole>("CREATOR");

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { role: "CREATOR" } });

  const onSubmit = async (values: FormValues) => {
    setServerError(null);
    try {
      const user = await registerAccount(values);
      navigate(user.role === "BUSINESS" ? "/business/profile" : "/creator/profile", { replace: true });
    } catch (err) {
      setServerError(extractErrorMessage(err, "Could not create your account"));
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-paper px-4 py-12">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <h1 className="font-display text-2xl font-semibold text-ink-900">Join Escobar.Club</h1>
          <p className="mt-1 text-sm text-ink-400">Set up your account to get started</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="card-surface flex flex-col gap-4 p-7">
          {serverError && (
            <div className="rounded-lg border border-danger-200 bg-danger-soft px-3 py-2 text-sm text-danger-deep">
              {serverError}
            </div>
          )}

          <div className="grid grid-cols-2 gap-2 rounded-lg bg-ink-50 p-1">
            {(["CREATOR", "BUSINESS"] as UserRole[]).map((r) => (
              <button
                key={r}
                type="button"
                onClick={() => {
                  setRole(r);
                  setValue("role", r as "CREATOR" | "BUSINESS");
                }}
                className={clsx(
                  "focus-ring rounded-md py-2 text-sm font-semibold transition-colors",
                  role === r ? "bg-white text-ink-900 shadow-sm" : "text-ink-400 hover:text-ink-600",
                )}
              >
                {r === "CREATOR" ? "I'm a Creator" : "I'm a Business"}
              </button>
            ))}
          </div>

          <Input
            label={role === "CREATOR" ? "Display name" : "Company Legal Name"}
            placeholder={role === "CREATOR" ? "Jamie Rivera" : "Acme Studios Pvt. Ltd."}
            error={errors.displayName?.message}
            {...register("displayName")}
          />
          {role === "BUSINESS" && (
            <>
              <Input label="GST Number" placeholder="22AAAAA0000A1Z5" error={errors.gstNumber?.message} {...register("gstNumber")} />
              <Input
                label="Contact Person Name"
                placeholder="Jamie Rivera"
                error={errors.contactPersonName?.message}
                {...register("contactPersonName")}
              />
              <Input
                label="Mobile Number"
                type="tel"
                placeholder="+91 98765 43210"
                error={errors.mobileNumber?.message}
                {...register("mobileNumber")}
              />
            </>
          )}
          <Input label="Email" type="email" placeholder="you@company.com" error={errors.email?.message} {...register("email")} />
          <Input
            label="Password"
            type="password"
            placeholder="At least 8 characters"
            error={errors.password?.message}
            {...register("password")}
          />
          <Button type="submit" isLoading={isSubmitting} className="mt-2 w-full">
            Create account
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-ink-400">
          Already have an account?{" "}
          <Link to="/login" className="font-medium text-signal-600 hover:text-signal-700">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
