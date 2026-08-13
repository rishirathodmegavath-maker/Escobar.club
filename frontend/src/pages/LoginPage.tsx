import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { GoogleLogin } from "@react-oauth/google";
import axios from "axios";
import { useAuth } from "@/auth/AuthContext";
import { Button } from "@/components/Button";
import { Input } from "@/components/Field";
import { extractErrorMessage } from "@/api/client";
import { HelpCircleIcon } from "@/components/icons";
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

const otpEmailSchema = z.object({
  email: z.string().email("Enter a valid email address"),
});
type OtpEmailFormValues = z.infer<typeof otpEmailSchema>;

const otpCodeSchema = z.object({
  code: z.string().regex(/^\d{6}$/, "Enter the 6-digit code"),
});
type OtpCodeFormValues = z.infer<typeof otpCodeSchema>;

const UNVERIFIED_MESSAGE = "Please verify your email before signing in";

type OtpStep = "idle" | "email" | "code";

const ADMIN_MUST_USE_ADMIN_LOGIN = "Admins must sign in at /admin/login";

export function LoginPage() {
  const { login, verifyTwoFactor, requestOtp, loginWithOtp, loginWithGoogle, resendVerification, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [serverError, setServerError] = useState<string | null>(null);
  const [showResend, setShowResend] = useState(false);
  const [resendState, setResendState] = useState<"idle" | "sending" | "sent">("idle");
  const [challengeToken, setChallengeToken] = useState<string | null>(null);
  const [otpStep, setOtpStep] = useState<OtpStep>("idle");
  const [otpEmail, setOtpEmail] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const {
    register: registerCode,
    handleSubmit: handleCodeSubmit,
    formState: { errors: codeErrors, isSubmitting: isVerifyingCode },
  } = useForm<TwoFactorFormValues>({ resolver: zodResolver(twoFactorSchema) });

  const {
    register: registerOtpEmail,
    handleSubmit: handleOtpEmailSubmit,
    formState: { errors: otpEmailErrors, isSubmitting: isSendingOtp },
  } = useForm<OtpEmailFormValues>({ resolver: zodResolver(otpEmailSchema) });

  const {
    register: registerOtpCode,
    handleSubmit: handleOtpCodeSubmit,
    formState: { errors: otpCodeErrors, isSubmitting: isVerifyingOtp },
  } = useForm<OtpCodeFormValues>({ resolver: zodResolver(otpCodeSchema) });

  const goToDestination = (user: { role: string }) => {
    // Admin accounts have their own dedicated sign-in at /admin/login; the public login page
    // must not double as an entry point for them.
    if (user.role === "ADMIN") {
      logout();
      setServerError(ADMIN_MUST_USE_ADMIN_LOGIN);
      return;
    }
    const from = (location.state as { from?: Location })?.from?.pathname;
    navigate(from ?? (user.role === "BUSINESS" ? "/business/content" : "/"), { replace: true });
  };

  const resetToPasswordMode = () => {
    setChallengeToken(null);
    setOtpStep("idle");
    setOtpEmail(null);
    setServerError(null);
  };

  const onSubmit = async (values: FormValues) => {
    setServerError(null);
    setShowResend(false);
    setResendState("idle");
    try {
      const result = await login(values);
      if (result.status === "twoFactorRequired") {
        setChallengeToken(result.challengeToken);
      } else {
        goToDestination(result.user);
      }
    } catch (err) {
      const message = extractErrorMessage(err, "Invalid email or password");
      setServerError(message);
      setShowResend(axios.isAxiosError(err) && err.response?.status === 403 && message === UNVERIFIED_MESSAGE);
    }
  };

  const onSubmitCode = async (values: TwoFactorFormValues) => {
    if (!challengeToken) return;
    setServerError(null);
    try {
      const user = await verifyTwoFactor({ challengeToken, code: values.code });
      goToDestination(user);
    } catch (err) {
      setServerError(extractErrorMessage(err, "Invalid or expired code"));
    }
  };

  const onRequestOtp = async (values: OtpEmailFormValues) => {
    setServerError(null);
    try {
      await requestOtp({ email: values.email });
      setOtpEmail(values.email);
      setOtpStep("code");
    } catch (err) {
      setServerError(extractErrorMessage(err, "Could not send a code. Please try again."));
    }
  };

  const onVerifyOtp = async (values: OtpCodeFormValues) => {
    if (!otpEmail) return;
    setServerError(null);
    try {
      const result = await loginWithOtp({ email: otpEmail, code: values.code });
      if (result.status === "twoFactorRequired") {
        setOtpStep("idle");
        setChallengeToken(result.challengeToken);
      } else {
        goToDestination(result.user);
      }
    } catch (err) {
      setServerError(extractErrorMessage(err, "Invalid or expired code"));
    }
  };

  const onResendVerification = async () => {
    setResendState("sending");
    try {
      await resendVerification({ email: getValues("email") });
    } finally {
      setResendState("sent");
    }
  };

  const onGoogleSuccess = async (credentialResponse: { credential?: string }) => {
    if (!credentialResponse.credential) return;
    setServerError(null);
    try {
      const result = await loginWithGoogle({ idToken: credentialResponse.credential });
      if (result.status === "twoFactorRequired") {
        setChallengeToken(result.challengeToken);
      } else {
        goToDestination(result.user);
      }
    } catch (err) {
      setServerError(extractErrorMessage(err, "Could not sign in with Google"));
    }
  };

  const heading = challengeToken
    ? "Two-factor verification"
    : otpStep === "email"
      ? "Sign in with a code"
      : otpStep === "code"
        ? "Enter your code"
        : "Welcome back";

  const subheading = challengeToken
    ? "Enter the 6-digit code from your authenticator app"
    : otpStep === "email"
      ? "We'll email you a one-time sign-in code"
      : otpStep === "code"
        ? `We sent a 6-digit code to ${otpEmail}`
        : "Sign in to manage partnerships on Escobar.Club";

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-paper px-4">
      <Link
        to="/help"
        aria-label="Help for creators"
        title="Help for creators"
        className="focus-ring absolute right-5 top-5 flex h-10 w-10 items-center justify-center rounded-full border border-ink-200 bg-surface text-ink-500 shadow-card hover:border-signal-300 hover:text-signal-deep"
      >
        <HelpCircleIcon className="h-5 w-5" />
      </Link>

      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-ink-950">
            <SignalMark size={26} />
          </div>
          <h1 className="font-display text-2xl font-semibold text-ink-900">{heading}</h1>
          <p className="mt-1 text-sm text-ink-400">{subheading}</p>
        </div>

        {challengeToken ? (
          <form onSubmit={handleCodeSubmit(onSubmitCode)} className="card-surface flex flex-col gap-4 p-7">
            {serverError && (
              <div className="rounded-lg border border-danger-200 bg-danger-soft px-3 py-2 text-sm text-danger-deep">
                <p>{serverError}</p>
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
            <button type="button" onClick={resetToPasswordMode} className="text-center text-xs font-medium text-signal-600 hover:text-signal-700">
              Back to sign in
            </button>
          </form>
        ) : otpStep === "email" ? (
          <form onSubmit={handleOtpEmailSubmit(onRequestOtp)} className="card-surface flex flex-col gap-4 p-7">
            {serverError && (
              <div className="rounded-lg border border-danger-200 bg-danger-soft px-3 py-2 text-sm text-danger-deep">
                <p>{serverError}</p>
              </div>
            )}
            <Input
              label="Email"
              type="email"
              placeholder="you@company.com"
              error={otpEmailErrors.email?.message}
              {...registerOtpEmail("email")}
            />
            <Button type="submit" isLoading={isSendingOtp} className="mt-2 w-full">
              Send code
            </Button>
            <button type="button" onClick={resetToPasswordMode} className="text-center text-xs font-medium text-signal-600 hover:text-signal-700">
              Back to sign in
            </button>
          </form>
        ) : otpStep === "code" ? (
          <form onSubmit={handleOtpCodeSubmit(onVerifyOtp)} className="card-surface flex flex-col gap-4 p-7">
            {serverError && (
              <div className="rounded-lg border border-danger-200 bg-danger-soft px-3 py-2 text-sm text-danger-deep">
                <p>{serverError}</p>
              </div>
            )}
            <Input
              label="6-digit code"
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              placeholder="123456"
              error={otpCodeErrors.code?.message}
              {...registerOtpCode("code")}
            />
            <Button type="submit" isLoading={isVerifyingOtp} className="mt-2 w-full">
              Sign in
            </Button>
            <button
              type="button"
              onClick={() => otpEmail && onRequestOtp({ email: otpEmail })}
              className="text-center text-xs font-medium text-signal-600 hover:text-signal-700"
            >
              Resend code
            </button>
            <button type="button" onClick={resetToPasswordMode} className="text-center text-xs font-medium text-ink-400 hover:text-ink-600">
              Back to sign in
            </button>
          </form>
        ) : (
          <form onSubmit={handleSubmit(onSubmit)} className="card-surface flex flex-col gap-4 p-7">
            {serverError && (
              <div className="rounded-lg border border-danger-200 bg-danger-soft px-3 py-2 text-sm text-danger-deep">
                <p>{serverError}</p>
                {showResend && (
                  <p className="mt-1">
                    {resendState === "sent" ? (
                      "If that account needs verifying, we've sent a new link."
                    ) : (
                      <button
                        type="button"
                        onClick={onResendVerification}
                        disabled={resendState === "sending"}
                        className="font-medium underline disabled:opacity-50"
                      >
                        Resend verification email
                      </button>
                    )}
                  </p>
                )}
              </div>
            )}
            <Input label="Email" type="email" placeholder="you@company.com" error={errors.email?.message} {...register("email")} />
            <div className="flex flex-col gap-1.5">
              <Input label="Password" type="password" placeholder="••••••••" error={errors.password?.message} {...register("password")} />
              <Link to="/forgot-password" className="self-end text-xs font-medium text-signal-600 hover:text-signal-700">
                Forgot password?
              </Link>
            </div>
            <Button type="submit" isLoading={isSubmitting} className="mt-2 w-full">
              Sign in
            </Button>

            <button
              type="button"
              onClick={() => {
                setServerError(null);
                setOtpStep("email");
              }}
              className="text-center text-xs font-medium text-signal-600 hover:text-signal-700"
            >
              Sign in with an emailed code instead
            </button>

            <div className="flex items-center gap-3 text-xs text-ink-300">
              <span className="h-px flex-1 bg-ink-100" />
              OR
              <span className="h-px flex-1 bg-ink-100" />
            </div>

            <div className="flex justify-center">
              <GoogleLogin onSuccess={onGoogleSuccess} onError={() => setServerError("Could not sign in with Google")} />
            </div>
          </form>
        )}

        <p className="mt-6 text-center text-sm text-ink-400">
          New to Escobar.Club?{" "}
          <Link to="/register" className="font-medium text-signal-600 hover:text-signal-700">
            Create an account
          </Link>
        </p>
      </div>
    </div>
  );
}
