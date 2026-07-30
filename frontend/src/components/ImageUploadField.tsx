import { useRef, useState } from "react";
import clsx from "clsx";
import { extractErrorMessage } from "@/api/client";
import { Spinner } from "@/components/Spinner";

interface ImageUploadFieldProps {
  label: string;
  value: string | null | undefined;
  onChange: (url: string) => void;
  uploadFn: (file: File, onProgress?: (percent: number) => void) => Promise<{ url: string }>;
  shape?: "square" | "circle";
}

export function ImageUploadField({ label, value, onChange, uploadFn, shape = "square" }: ImageUploadFieldProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [progress, setProgress] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleFile = async (file: File) => {
    setError(null);
    setProgress(0);
    try {
      const result = await uploadFn(file, setProgress);
      onChange(result.url);
    } catch (err) {
      setError(extractErrorMessage(err, "Upload failed"));
    } finally {
      setProgress(null);
    }
  };

  return (
    <div className="flex flex-col gap-2">
      <span className="text-sm font-medium text-ink-700">{label}</span>
      <div
        onClick={() => inputRef.current?.click()}
        onDragOver={(e) => e.preventDefault()}
        onDrop={(e) => {
          e.preventDefault();
          const file = e.dataTransfer.files?.[0];
          if (file) handleFile(file);
        }}
        className={clsx(
          "focus-ring flex h-24 w-24 shrink-0 cursor-pointer items-center justify-center overflow-hidden border-2 border-dashed border-ink-200 bg-paper-100 transition-colors hover:border-signal-300 hover:bg-signal-50/40",
          shape === "circle" ? "rounded-full" : "rounded-xl",
        )}
      >
        {progress !== null ? (
          <Spinner />
        ) : value ? (
          <img src={value} alt="" className="h-full w-full object-cover" onError={(e) => (e.currentTarget.style.display = "none")} />
        ) : (
          <span className="px-2 text-center text-[11px] leading-tight text-ink-400">Click or drop image</span>
        )}
      </div>
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) handleFile(file);
        }}
      />
      {error && <span className="text-xs font-medium text-danger-500">{error}</span>}
    </div>
  );
}
