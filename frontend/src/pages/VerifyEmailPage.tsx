import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { extractErrorMessage } from "@/api/client";
import { Spinner } from "@/components/Spinner";

export function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const navigate = useNavigate();
  const { verifyEmail } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const calledRef = useRef(false);

  useEffect(() => {
    if (!token || calledRef.current) return;
    calledRef.current = true;

    verifyEmail({ token })
      .then((user) => {
        navigate(user.role === "BUSINESS" ? "/business/content" : "/", { replace: true });
      })
      .catch((err) => {
        setError(extractErrorMessage(err, "This verification link is invalid or has expired"));
      });
  }, [token, verifyEmail, navigate]);

  if (!token || error) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-paper px-4">
        <div className="w-full max-w-md text-center">
          <div className="card-surface flex flex-col gap-3 p-7">
            <h1 className="font-display text-xl font-semibold text-ink-900">
              {token ? "This verification link is invalid" : "Missing verification link"}
            </h1>
            <p className="text-sm text-ink-400">{error ?? "Please use the link from your verification email."}</p>
            <Link to="/login" className="mt-2 font-medium text-signal-600 hover:text-signal-700">
              Back to sign in
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-paper px-4 text-center">
      <Spinner size={28} />
      <p className="text-sm text-ink-500">Verifying your email…</p>
    </div>
  );
}
