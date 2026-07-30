import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { Button } from "@/components/Button";
import { Input } from "@/components/Field";
import { extractErrorMessage } from "@/api/client";
import { HelpCircleIcon } from "@/components/icons";

const schema = z.object({
  email: z.string().email("Enter a valid email address"),
  password: z.string().min(1, "Password is required"),
});
type FormValues = z.infer<typeof schema>;

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const onSubmit = async (values: FormValues) => {
    setServerError(null);
    try {
      const user = await login(values);
      const from = (location.state as { from?: Location })?.from?.pathname;
      navigate(from ?? (user.role === "BUSINESS" ? "/business/content" : "/"), { replace: true });
    } catch (err) {
      setServerError(extractErrorMessage(err, "Invalid email or password"));
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-paper px-4">
      <Link
        to="/help"
        aria-label="Help for creators"
        title="Help for creators"
        className="focus-ring absolute right-5 top-5 flex h-10 w-10 items-center justify-center rounded-full border border-ink-200 bg-white text-ink-500 shadow-card hover:border-signal-300 hover:text-signal-700"
      >
        <HelpCircleIcon className="h-5 w-5" />
      </Link>

      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-gold-400 text-ink-900">
            <svg width="22" height="22" viewBox="0 0 32 32" fill="none">
              <path d="M9 21.5V10.5H18.5" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
              <path d="M9 16H16.5" stroke="#2FBE9A" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </div>
          <h1 className="font-display text-2xl font-semibold text-ink-900">Welcome back</h1>
          <p className="mt-1 text-sm text-ink-400">Sign in to manage partnerships on Escobar.Club</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="card-surface flex flex-col gap-4 p-7">
          {serverError && (
            <div className="rounded-lg border border-danger-200 bg-danger-soft px-3 py-2 text-sm text-danger-deep">
              {serverError}
            </div>
          )}
          <Input label="Email" type="email" placeholder="you@company.com" error={errors.email?.message} {...register("email")} />
          <Input label="Password" type="password" placeholder="••••••••" error={errors.password?.message} {...register("password")} />
          <Button type="submit" isLoading={isSubmitting} className="mt-2 w-full">
            Sign in
          </Button>
        </form>

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
