import { Link } from "react-router-dom";
import { Button } from "@/components/Button";
import type { CreatorKycProfile } from "@/types";

interface NoticeCopy {
  icon: string;
  title: string;
  body: string;
  showCompleteKycButton: boolean;
}

function copyFor(kyc: CreatorKycProfile | undefined): NoticeCopy {
  if (!kyc) {
    return {
      icon: "🔒",
      title: "KYC verification required",
      body: "Complete and verify your KYC to participate in campaigns and submit content.",
      showCompleteKycButton: true,
    };
  }
  if (kyc.eligibleToParticipate) {
    // Callers should not render this component when eligible - guarded here only as a fallback.
    return {
      icon: "✅",
      title: "KYC verified",
      body: "You're verified and eligible to participate in campaigns.",
      showCompleteKycButton: false,
    };
  }
  if (kyc.status === "REJECTED") {
    return {
      icon: "⚠️",
      title: "KYC verification required",
      body: `Your KYC verification was not approved${kyc.reviewNote ? `: ${kyc.reviewNote}` : "."} Please update your documents and resubmit.`,
      showCompleteKycButton: true,
    };
  }
  if (kyc.status === "VERIFIED") {
    // Verified by a business's peer review only - still awaiting an admin's sign-off before
    // campaign participation unlocks platform-wide.
    return {
      icon: "⏳",
      title: "KYC verification pending",
      body: "You can explore campaigns while your verification is being finalized by our team.",
      showCompleteKycButton: false,
    };
  }
  return {
    icon: "⏳",
    title: "KYC verification pending",
    body: "You can explore campaigns while your verification is being reviewed.",
    showCompleteKycButton: false,
  };
}

export function KycGateNotice({ kyc }: { kyc: CreatorKycProfile | undefined }) {
  const copy = copyFor(kyc);

  return (
    <div className="flex flex-col items-start gap-3 rounded-xl border border-surface-border bg-surface-hover p-5">
      <p className="font-display text-base font-semibold text-ink-900">
        {copy.icon} {copy.title}
      </p>
      <p className="text-sm text-ink-500">{copy.body}</p>
      {copy.showCompleteKycButton && (
        <Link to="/creator/kyc">
          <Button size="sm">Complete KYC</Button>
        </Link>
      )}
    </div>
  );
}
