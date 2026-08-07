import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link, useNavigate } from "react-router-dom";
import clsx from "clsx";
import { GoogleLogin } from "@react-oauth/google";
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
    confirmPassword: z.string(),
    gstNumber: z.string().max(20).optional().or(z.literal("")),
    contactPersonName: z.string().max(150).optional().or(z.literal("")),
    mobileNumber: z.string().max(20).optional().or(z.literal("")),
  })
  .superRefine((data, ctx) => {
    if (data.password !== data.confirmPassword) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: "Passwords do not match", path: ["confirmPassword"] });
    }
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
  const { register: registerAccount, loginWithGoogle } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);
  const [role, setRole] = useState<UserRole>("CREATOR");
  const [submittedEmail, setSubmittedEmail] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { role: "CREATOR" } });

  const goToDestination = (user: { role: string }) => {
    navigate(user.role === "BUSINESS" ? "/business/profile" : "/creator/profile", { replace: true });
  };

  const onSubmit = async (values: FormValues) => {
    setServerError(null);
    try {
      await registerAccount(values);
      setSubmittedEmail(values.email);
    } catch (err) {
      setServerError(extractErrorMessage(err, "Could not create your account"));
    }
  };

  const onGoogleSuccess = async (credentialResponse: { credential?: string }) => {
    if (!credentialResponse.credential) return;
    setServerError(null);
    try {
      const values = getValues();
      const result = await loginWithGoogle({
        idToken: credentialResponse.credential,
        role: values.role,
        displayName: values.displayName,
        gstNumber: values.gstNumber,
        contactPersonName: values.contactPersonName,
        mobileNumber: values.mobileNumber,
      });
      if (result.status === "twoFactorRequired") {
        setServerError("This account has two-factor authentication enabled. Please sign in from the Sign in page.");
      } else {
        goToDestination(result.user);
      }
    } catch (err) {
      setServerError(extractErrorMessage(err, "Could not sign in with Google"));
    }
  };

  if (submittedEmail) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-paper px-4 py-12">
        <div className="w-full max-w-md">
          <div className="mb-8 text-center">
            <h1 className="font-display text-2xl font-semibold text-ink-900">Check your email</h1>
            <p className="mt-1 text-sm text-ink-400">One more step to activate your account</p>
          </div>
          <div className="card-surface flex flex-col gap-3 p-7 text-sm text-ink-700">
            <p>
              We've sent a verification link to <span className="font-medium text-ink-900">{submittedEmail}</span>.
              Click it to activate your account and sign in.
            </p>
            <p className="text-ink-400">Didn't get it? Check your spam folder, or try signing in once you find it.</p>
          </div>
          <p className="mt-6 text-center text-sm text-ink-400">
            <Link to="/login" className="font-medium text-signal-600 hover:text-signal-700">
              Back to sign in
            </Link>
          </p>
        </div>
      </div>
    );
  }

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
                  role === r
                    ? "bg-gradient-to-br from-signal-500 to-signal-800 text-white shadow-[0_4px_18px_rgba(250,35,59,0.4)]"
                    : "text-ink-400 hover:text-ink-700",
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
          <Input
            label="Confirm Password"
            type="password"
            placeholder="Re-enter your password"
            error={errors.confirmPassword?.message}
            {...register("confirmPassword")}
          />
          <Button type="submit" isLoading={isSubmitting} className="mt-2 w-full">
            Create account
          </Button>

          <div className="flex items-center gap-3 text-xs text-ink-300">
            <span className="h-px flex-1 bg-ink-100" />
            OR
            <span className="h-px flex-1 bg-ink-100" />
          </div>

          <div className="flex justify-center">
            <GoogleLogin onSuccess={onGoogleSuccess} onError={() => setServerError("Could not sign in with Google")} />
          </div>
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
