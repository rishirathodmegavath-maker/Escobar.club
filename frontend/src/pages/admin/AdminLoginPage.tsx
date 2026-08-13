import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { Button } from "@/components/Button";
import { Input } from "@/components/Field";
import { extractErrorMessage } from "@/api/client";
import { SignalMark } from "@/components/SignalMark";

const schema = z.object({
  email: z.string().email("Enter a valid email address"),
  password: z.string().min(1, "Password is required"),
});
type FormValues = z.infer<typeof schema>;

const twoFactorSchema = z.object({
  code: z.string().regex(/^\d{6}$/, "Enter the 6-digit code"),
});
type TwoFactorFormValues = z.infer<typeof twoFactorSchema>;

const NOT_ADMIN_MESSAGE = "This sign-in is for administrators only";

export function AdminLoginPage() {
  const { login, verifyTwoFactor, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [serverError, setServerError] = useState<string | null>(null);
  const [challengeToken, setChallengeToken] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const {
    register: registerCode,
    handleSubmit: handleCodeSubmit,
    formState: { errors: codeErrors, isSubmitting: isVerifyingCode },
  } = useForm<TwoFactorFormValues>({ resolver: zodResolver(twoFactorSchema) });

  const goToAdmin = (user: { role: string }) => {
    // Backend already enforces ADMIN on every /api/admin/** route; this is a UX guard so a
    // non-admin credential typed here doesn't silently land the user in a half-broken session.
    if (user.role !== "ADMIN") {
      logout();
      setServerError(NOT_ADMIN_MESSAGE);
      return;
    }
    const from = (location.state as { from?: Location })?.from?.pathname;
    navigate(from && from.startsWith("/admin") ? from : "/admin", { replace: true });
  };

  const onSubmit = async (values: FormValues) => {
    setServerError(null);
    try {
      const result = await login(values);
      if (result.status === "twoFactorRequired") {
        setChallengeToken(result.challengeToken);
      } else {
        goToAdmin(result.user);
      }
    } catch (err) {
      setServerError(extractErrorMessage(err, "Invalid email or password"));
    }
  };

  const onSubmitCode = async (values: TwoFactorFormValues) => {
    if (!challengeToken) return;
    setServerError(null);
    try {
      const user = await verifyTwoFactor({ challengeToken, code: values.code });
      goToAdmin(user);
    } catch (err) {
      setServerError(extractErrorMessage(err, "Invalid or expired code"));
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-ink-950 px-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-surface">
            <SignalMark size={26} />
          </div>
          <h1 className="font-display text-2xl font-semibold text-white">
            {challengeToken ? "Two-factor verification" : "Admin sign-in"}
          </h1>
          <p className="mt-1 text-sm text-ink-300">
            {challengeToken ? "Enter the 6-digit code from your authenticator app" : "Escobar.Club administration"}
          </p>
        </div>

        {challengeToken ? (
          <form onSubmit={handleCodeSubmit(onSubmitCode)} className="card-surface flex flex-col gap-4 p-7">
            {serverError && (
              <div className="rounded-lg border border-danger-200 bg-danger-soft px-3 py-2 text-sm text-danger-deep">
                {serverError}
              </div>
            )}
            <Input
              label="6-digit code"
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              placeholder="123456"
              error={codeErrors.code?.message}
              {...registerCode("code")}
            />
            <Button type="submit" isLoading={isVerifyingCode} className="mt-2 w-full">
              Verify and sign in
            </Button>
            <button
              type="button"
              onClick={() => {
                setChallengeToken(null);
                setServerError(null);
              }}
              className="text-center text-xs font-medium text-signal-600 hover:text-signal-700"
            >
              Back to sign in
            </button>
          </form>
        ) : (
          <form onSubmit={handleSubmit(onSubmit)} className="card-surface flex flex-col gap-4 p-7">
            {serverError && (
              <div className="rounded-lg border border-danger-200 bg-danger-soft px-3 py-2 text-sm text-danger-deep">
                {serverError}
              </div>
            )}
            <Input label="Email" type="email" placeholder="admin@example.com" error={errors.email?.message} {...register("email")} />
            <div className="flex flex-col gap-1.5">
              <Input label="Password" type="password" placeholder="••••••••" error={errors.password?.message} {...register("password")} />
              <Link to="/forgot-password" className="self-end text-xs font-medium text-signal-600 hover:text-signal-700">
                Forgot password?
              </Link>
            </div>
            <Button type="submit" isLoading={isSubmitting} className="mt-2 w-full">
              Sign in
            </Button>
          </form>
        )}
      </div>
    </div>
  );
}
