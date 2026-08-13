import { useRef, useState } from "react";
import { contentApi } from "@/api/content";
import { extractErrorMessage } from "@/api/client";
import type { MediaType } from "@/types";
import { Spinner } from "@/components/Spinner";

interface MediaUploadFieldProps {
  value: { url: string; mediaType: MediaType } | null;
  onChange: (value: { url: string; mediaType: MediaType } | null) => void;
  error?: string;
}

export function MediaUploadField({ value, onChange, error }: MediaUploadFieldProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [progress, setProgress] = useState<number | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const handleFile = async (file: File) => {
    setUploadError(null);
    setProgress(0);
    try {
      const mediaType: MediaType = file.type.startsWith("video/") ? "VIDEO" : "IMAGE";
      const result = await contentApi.upload(file, setProgress);
      onChange({ url: result.url, mediaType });
    } catch (err) {
      setUploadError(extractErrorMessage(err, "Upload failed"));
    } finally {
      setProgress(null);
    }
  };

  return (
    <div className="flex flex-col gap-2">
      <span className="text-sm font-medium text-ink-700">Media</span>
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
            {value.mediaType === "IMAGE" ? (
              <img src={value.url} alt="" className="max-h-48 rounded-lg object-cover" />
            ) : (
              <video src={value.url} controls className="max-h-48 rounded-lg" />
            )}
            <p className="text-xs font-medium text-signal-600">Click to replace</p>
          </>
        ) : (
          <>
            <p className="text-sm font-medium text-ink-600">Click to upload, or drag a file here</p>
            <p className="text-xs text-ink-400">Images or videos, up to 200MB</p>
          </>
        )}
      </div>
      <input
        ref={inputRef}
        type="file"
        accept="image/*,video/*"
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
