import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { QRCodeSVG } from "qrcode.react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { accountApi } from "@/api/account";
import { extractErrorMessage, tokenStorage } from "@/api/client";
import { useAuth } from "@/auth/AuthContext";
import { useToast } from "@/components/Toast";
import { Button } from "@/components/Button";
import { Card, CardHeader } from "@/components/Card";
import { Input } from "@/components/Field";
import { Spinner } from "@/components/Spinner";
import type { TwoFactorSetupResponse } from "@/types";

const codeSchema = z.object({ code: z.string().regex(/^\d{6}$/, "Enter the 6-digit code") });
type CodeFormValues = z.infer<typeof codeSchema>;

export function SecuritySettingsPage() {
  const { user } = useAuth();
  const { push } = useToast();
  const queryClient = useQueryClient();

  const [twoFactorEnabled, setTwoFactorEnabled] = useState(user?.twoFactorEnabled ?? false);
  const [setupData, setSetupData] = useState<TwoFactorSetupResponse | null>(null);
  const [disabling, setDisabling] = useState(false);

  const confirmForm = useForm<CodeFormValues>({ resolver: zodResolver(codeSchema) });
  const disableForm = useForm<CodeFormValues>({ resolver: zodResolver(codeSchema) });

  const setupMutation = useMutation({
    mutationFn: accountApi.setupTwoFactor,
    onSuccess: (data) => setSetupData(data),
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  const confirmMutation = useMutation({
    mutationFn: (code: string) => accountApi.confirmTwoFactor(code),
    onSuccess: () => {
      setTwoFactorEnabled(true);
      setSetupData(null);
      confirmForm.reset();
      push("Two-factor authentication enabled", "success");
    },
    onError: (err) => push(extractErrorMessage(err, "Invalid or expired code"), "error"),
  });

  const disableMutation = useMutation({
    mutationFn: (code: string) => accountApi.disableTwoFactor(code),
    onSuccess: () => {
      setTwoFactorEnabled(false);
      setDisabling(false);
      disableForm.reset();
      push("Two-factor authentication disabled", "success");
    },
    onError: (err) => push(extractErrorMessage(err, "Invalid or expired code"), "error"),
  });

  const sessionsQuery = useQuery({ queryKey: ["account", "sessions"], queryFn: accountApi.listSessions });

  const revokeMutation = useMutation({
    mutationFn: (id: number) => accountApi.revokeSession(id),
    onSuccess: () => {
      push("Session revoked", "success");
      queryClient.invalidateQueries({ queryKey: ["account", "sessions"] });
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  const currentSessionId = tokenStorage.getSessionId();

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink-900">Security</h1>
        <p className="mt-1.5 text-ink-500">Two-factor authentication and active sessions for your account.</p>
      </div>

      <Card>
        <CardHeader>
          <div>
            <h2 className="text-lg font-semibold text-ink-900">Two-factor authentication</h2>
            <p className="text-sm text-ink-500">Require a code from an authenticator app when signing in.</p>
          </div>
        </CardHeader>

        {twoFactorEnabled ? (
          disabling ? (
            <form onSubmit={disableForm.handleSubmit((v) => disableMutation.mutate(v.code))} className="flex flex-col gap-3">
              <Input
                label="Enter a code to confirm disabling"
                inputMode="numeric"
                maxLength={6}
                placeholder="123456"
                error={disableForm.formState.errors.code?.message}
                {...disableForm.register("code")}
              />
              <div className="flex gap-2">
                <Button type="submit" variant="danger" isLoading={disableMutation.isPending}>
                  Disable two-factor authentication
                </Button>
                <Button type="button" variant="ghost" onClick={() => setDisabling(false)}>
                  Cancel
                </Button>
              </div>
            </form>
          ) : (
            <div className="flex items-center justify-between">
              <p className="text-sm font-medium text-mint-deep">Enabled</p>
              <Button variant="danger" onClick={() => setDisabling(true)}>
                Disable
              </Button>
            </div>
          )
        ) : setupData ? (
          <form onSubmit={confirmForm.handleSubmit((v) => confirmMutation.mutate(v.code))} className="flex flex-col gap-4">
            <div className="flex flex-col items-center gap-3 rounded-lg border border-ink-100 bg-ink-50 p-5">
              <QRCodeSVG value={setupData.otpauthUri} size={168} />
              <p className="text-center text-xs text-ink-400">
                Scan with your authenticator app, or enter this code manually:
                <br />
                <span className="font-mono text-ink-700">{setupData.secret}</span>
              </p>
            </div>
            <Input
              label="Enter the 6-digit code to confirm"
              inputMode="numeric"
              maxLength={6}
              placeholder="123456"
              error={confirmForm.formState.errors.code?.message}
              {...confirmForm.register("code")}
            />
            <div className="flex gap-2">
              <Button type="submit" isLoading={confirmMutation.isPending}>
                Confirm and enable
              </Button>
              <Button type="button" variant="ghost" onClick={() => setSetupData(null)}>
                Cancel
              </Button>
            </div>
          </form>
        ) : (
          <div className="flex items-center justify-between">
            <p className="text-sm text-ink-500">Not enabled</p>
            <Button onClick={() => setupMutation.mutate()} isLoading={setupMutation.isPending}>
              Enable two-factor authentication
            </Button>
          </div>
        )}
      </Card>

      <Card>
        <CardHeader>
          <div>
            <h2 className="text-lg font-semibold text-ink-900">Active sessions</h2>
            <p className="text-sm text-ink-500">Devices currently signed in to your account.</p>
          </div>
        </CardHeader>

        {sessionsQuery.isLoading ? (
          <div className="flex justify-center py-6">
            <Spinner size={24} />
          </div>
        ) : (
          <div className="flex flex-col divide-y divide-ink-100">
            {(sessionsQuery.data ?? []).map((session) => (
              <div key={session.id} className="flex items-center justify-between gap-4 py-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-ink-900">
                    {session.ipAddress ?? "Unknown IP"}
                    {session.id === currentSessionId && (
                      <span className="ml-2 rounded-full bg-signal-soft px-2 py-0.5 text-[11px] font-semibold uppercase text-signal-deep">
                        This device
                      </span>
                    )}
                  </p>
                  <p className="truncate text-xs text-ink-400">{session.userAgent ?? "Unknown device"}</p>
                  <p className="text-xs text-ink-300">
                    Signed in {new Date(session.createdAt).toLocaleString()}
                    {session.lastUsedAt && ` · last used ${new Date(session.lastUsedAt).toLocaleString()}`}
                  </p>
                </div>
                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => {
                    if (window.confirm("Revoke this session? That device will be signed out.")) {
                      revokeMutation.mutate(session.id);
                    }
                  }}
                  isLoading={revokeMutation.isPending && revokeMutation.variables === session.id}
                >
                  Revoke
                </Button>
              </div>
            ))}
            {(sessionsQuery.data ?? []).length === 0 && <p className="py-3 text-sm text-ink-400">No active sessions found.</p>}
          </div>
        )}
      </Card>
    </div>
  );
}
