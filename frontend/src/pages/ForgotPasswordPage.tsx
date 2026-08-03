import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { Button } from "@/components/Button";
import { Input } from "@/components/Field";

const schema = z.object({
  email: z.string().email("Enter a valid email address"),
});
type FormValues = z.infer<typeof schema>;

export function ForgotPasswordPage() {
  const { forgotPassword } = useAuth();
  const [submitted, setSubmitted] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const onSubmit = async (values: FormValues) => {
    try {
      await forgotPassword(values);
    } catch {
      // fall through - always show the same success state, never reveal whether the email exists
    } finally {
      setSubmitted(true);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-paper px-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <h1 className="font-display text-2xl font-semibold text-ink-900">Forgot password</h1>
          <p className="mt-1 text-sm text-ink-400">We'll email you a link to reset it</p>
        </div>

        <div className="card-surface flex flex-col gap-4 p-7">
          {submitted ? (
            <p className="text-sm text-ink-700">
              If an account exists for that email, we've sent a password reset link. Check your inbox and follow the
              link to set a new password.
            </p>
          ) : (
            <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
              <Input
                label="Email"
                type="email"
                placeholder="you@company.com"
                error={errors.email?.message}
                {...register("email")}
              />
              <Button type="submit" isLoading={isSubmitting} className="mt-2 w-full">
                Send reset link
              </Button>
            </form>
          )}
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
