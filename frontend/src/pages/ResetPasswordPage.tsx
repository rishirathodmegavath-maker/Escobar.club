import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { Button } from "@/components/Button";
import { Input } from "@/components/Field";
import { extractErrorMessage } from "@/api/client";

const schema = z
  .object({
    newPassword: z.string().min(8, "Must be at least 8 characters"),
    confirmPassword: z.string(),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  });
type FormValues = z.infer<typeof schema>;

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const navigate = useNavigate();
  const { resetPassword } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const onSubmit = async (values: FormValues) => {
    if (!token) return;
    setServerError(null);
    try {
      await resetPassword({ token, newPassword: values.newPassword });
      navigate("/login", { replace: true });
    } catch (err) {
      setServerError(extractErrorMessage(err, "This password reset link is invalid or has expired"));
    }
  };

  if (!token) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-paper px-4">
        <div className="w-full max-w-md text-center">
          <div className="card-surface flex flex-col gap-3 p-7">
            <h1 className="font-display text-xl font-semibold text-ink-900">This reset link is invalid</h1>
            <p className="text-sm text-ink-400">Request a new one to continue.</p>
            <Link to="/forgot-password" className="mt-2 font-medium text-signal-600 hover:text-signal-700">
              Request a new link
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-paper px-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <h1 className="font-display text-2xl font-semibold text-ink-900">Set a new password</h1>
          <p className="mt-1 text-sm text-ink-400">Choose a new password for your account</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="card-surface flex flex-col gap-4 p-7">
          {serverError && (
            <div className="rounded-lg border border-danger-200 bg-danger-soft px-3 py-2 text-sm text-danger-deep">
              {serverError}{" "}
              <Link to="/forgot-password" className="font-medium underline">
                Request a new link
              </Link>
            </div>
          )}
          <Input
            label="New password"
            type="password"
            placeholder="At least 8 characters"
            error={errors.newPassword?.message}
            {...register("newPassword")}
          />
          <Input
            label="Confirm new password"
            type="password"
            placeholder="Re-enter your new password"
            error={errors.confirmPassword?.message}
            {...register("confirmPassword")}
          />
          <Button type="submit" isLoading={isSubmitting} className="mt-2 w-full">
            Reset password
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-ink-400">
          <Link to="/login" className="font-medium text-signal-600 hover:text-signal-700">
            Back to sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
