import { useEffect, useRef, useState } from "react";
import { kycApi } from "@/api/kyc";
import { extractErrorMessage } from "@/api/client";
import { Spinner } from "@/components/Spinner";

interface KycDocumentUploadFieldProps {
  value: string | null;
  onChange: (key: string) => void;
  error?: string;
  // Identifies the creator's already-submitted document (if any) so its preview can be fetched
  // through the authenticated endpoint - the raw file is no longer reachable by a plain URL.
  creatorId: number;
  hasExistingDocument: boolean;
}

export function KycDocumentUploadField({ value, onChange, error, creatorId, hasExistingDocument }: KycDocumentUploadFieldProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [progress, setProgress] = useState<number | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [previewIsPdf, setPreviewIsPdf] = useState(false);

  // Fetch the existing document's preview once (only when no fresh upload has happened yet in
  // this session - a fresh local pick already has its own object URL from handleFile below).
  useEffect(() => {
    if (value || !hasExistingDocument) return;
    let cancelled = false;
    let objectUrl: string | null = null;
    kycApi
      .fetchDocumentBlob(creatorId)
      .then((blob) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(blob);
        setPreviewIsPdf(blob.type === "application/pdf");
        setPreviewUrl(objectUrl);
      })
      .catch(() => {
        // Best-effort preview only - the form still works without it.
      });
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [creatorId, hasExistingDocument]);

  const handleFile = async (file: File) => {
    setUploadError(null);
    setProgress(0);
    const localUrl = URL.createObjectURL(file);
    setPreviewIsPdf(file.type === "application/pdf");
    setPreviewUrl(localUrl);
    try {
      const result = await kycApi.uploadDocument(file, setProgress);
      onChange(result.documentKey);
    } catch (err) {
      setUploadError(extractErrorMessage(err, "Upload failed"));
    } finally {
      setProgress(null);
    }
  };

  return (
    <div className="flex flex-col gap-2">
      <span className="text-sm font-medium text-ink-700">PAN card (photo or PDF)</span>
      <div
        onClick={() => inputRef.current?.click()}
        onDragOver={(e) => e.preventDefault()}
        onDrop={(e) => {
          e.preventDefault();
          const file = e.dataTransfer.files?.[0];
          if (file) handleFile(file);
        }}
        className="focus-ring flex cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed border-ink-200 bg-paper-100 px-4 py-8 text-center transition-colors hover:border-signal-300 hover:bg-signal-50/40"
      >
        {progress !== null ? (
          <>
            <Spinner />
            <p className="text-sm text-ink-500">Uploading… {progress}%</p>
          </>
        ) : previewUrl ? (
          <>
            {previewIsPdf ? (
              <div className="flex items-center gap-2 rounded-lg border border-surface-border bg-surface px-4 py-3 text-sm text-ink-700">
                📄 PAN document uploaded
              </div>
            ) : (
              <img src={previewUrl} alt="" className="max-h-48 rounded-lg object-cover" />
            )}
            <p className="text-xs font-medium text-signal-600">Click to replace</p>
          </>
        ) : (
          <>
            <p className="text-sm font-medium text-ink-600">Click to upload, or drag a file here</p>
            <p className="text-xs text-ink-400">A clear photo or PDF scan of your PAN card</p>
          </>
        )}
      </div>
      <input
        ref={inputRef}
        type="file"
        accept="image/*,application/pdf"
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) handleFile(file);
        }}
      />
      {(error || uploadError) && <span className="text-xs font-medium text-danger-500">{error || uploadError}</span>}
    </div>
  );
}
