import { useRef, useState } from "react";
import { kycApi } from "@/api/kyc";
import { extractErrorMessage } from "@/api/client";
import { Spinner } from "@/components/Spinner";

interface KycDocumentUploadFieldProps {
  value: string | null;
  onChange: (url: string) => void;
  error?: string;
}

export function KycDocumentUploadField({ value, onChange, error }: KycDocumentUploadFieldProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [progress, setProgress] = useState<number | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const handleFile = async (file: File) => {
    setUploadError(null);
    setProgress(0);
    try {
      const result = await kycApi.uploadDocument(file, setProgress);
      onChange(result.url);
    } catch (err) {
      setUploadError(extractErrorMessage(err, "Upload failed"));
    } finally {
      setProgress(null);
    }
  };

  const isImage = value ? /\.(png|jpe?g|webp|gif)$/i.test(value) || value.startsWith("data:image") : false;

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
        ) : value ? (
          <>
            {isImage ? (
              <img src={value} alt="" className="max-h-48 rounded-lg object-cover" />
            ) : (
              <div className="flex items-center gap-2 rounded-lg border border-surface-border bg-surface px-4 py-3 text-sm text-ink-700">
                📄 PAN document uploaded
              </div>
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
