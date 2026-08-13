import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { kycApi } from "@/api/kyc";
import { extractErrorMessage } from "@/api/client";
import { useToast } from "@/components/Toast";
import { Button } from "@/components/Button";
import { TextArea } from "@/components/Field";
import { StatusPill } from "@/components/StatusPill";
import { Spinner } from "@/components/Spinner";

export function KycReviewPanel({ creatorId }: { creatorId: number }) {
  const [reviewing, setReviewing] = useState<"VERIFIED" | "REJECTED" | null>(null);
  const [note, setNote] = useState("");
  const [openingDocument, setOpeningDocument] = useState(false);
  const queryClient = useQueryClient();
  const { push } = useToast();

  const openDocument = async () => {
    setOpeningDocument(true);
    try {
      const blob = await kycApi.fetchDocumentBlob(creatorId);
      const url = URL.createObjectURL(blob);
      window.open(url, "_blank", "noreferrer");
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (err) {
      push(extractErrorMessage(err, "Couldn't load the document"), "error");
    } finally {
      setOpeningDocument(false);
    }
  };

  const { data, isLoading } = useQuery({
    queryKey: ["kyc", "review", creatorId],
    queryFn: () => kycApi.getForReview(creatorId),
    retry: false,
  });

  const mutation = useMutation({
    mutationFn: (status: "VERIFIED" | "REJECTED") => kycApi.review(creatorId, { status, reviewNote: note || undefined }),
    onSuccess: (_, status) => {
      push(`KYC ${status === "VERIFIED" ? "verified" : "rejected"}.`, "success");
      queryClient.invalidateQueries({ queryKey: ["kyc", "review", creatorId] });
      setReviewing(null);
      setNote("");
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-4">
        <Spinner />
      </div>
    );
  }
  if (!data) {
    return <p className="border-t border-ink-100 pt-4 text-sm text-ink-400">This creator hasn't submitted KYC yet.</p>;
  }

  return (
    <div className="border-t border-ink-100 pt-4">
      <div className="flex items-center justify-between gap-3">
        <p className="text-xs font-semibold uppercase tracking-wide text-ink-400">KYC (PAN)</p>
        <StatusPill status={data.status} />
      </div>
      <div className="mt-2 grid grid-cols-2 gap-3 text-sm text-ink-600">
        <span>
          <span className="text-ink-400">PAN: </span>
          {data.panNumber}
        </span>
        <span>
          <span className="text-ink-400">Name: </span>
          {data.nameOnPan}
        </span>
      </div>
      {data.hasDocument && (
        <button
          type="button"
          onClick={openDocument}
          disabled={openingDocument}
          className="mt-2 inline-block text-sm text-signal-600 hover:underline disabled:opacity-50"
        >
          {openingDocument ? "Opening…" : "View uploaded document →"}
        </button>
      )}

      {data.status === "PENDING" && (
        <div className="mt-4">
          {reviewing ? (
            <div className="flex flex-col gap-3">
              <TextArea
                label={`Note (optional) — ${reviewing === "VERIFIED" ? "confirm details checked" : "explain why"}`}
                value={note}
                onChange={(e) => setNote(e.target.value)}
                rows={2}
              />
              <div className="flex gap-2">
                <Button
                  size="sm"
                  variant={reviewing === "VERIFIED" ? "primary" : "danger"}
                  isLoading={mutation.isPending}
                  onClick={() => mutation.mutate(reviewing)}
                >
                  Confirm {reviewing === "VERIFIED" ? "verification" : "rejection"}
                </Button>
                <Button size="sm" variant="ghost" onClick={() => setReviewing(null)}>
                  Cancel
                </Button>
              </div>
            </div>
          ) : (
            <div className="flex gap-2">
              <Button size="sm" onClick={() => setReviewing("VERIFIED")}>
                Verify KYC
              </Button>
              <Button size="sm" variant="danger" onClick={() => setReviewing("REJECTED")}>
                Reject
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
