import { useEffect, useRef, useState } from "react";
import clsx from "clsx";
import { extractErrorMessage } from "@/api/client";
import { Spinner } from "@/components/Spinner";
import { CameraIcon, EyeIcon, ImageStackIcon, TrashIcon } from "@/components/icons";
import { AvatarCropModal } from "@/components/AvatarCropModal";
import { ImageViewerModal } from "@/components/ImageViewerModal";

interface ImageUploadFieldProps {
  label: string;
  value: string | null | undefined;
  onChange: (url: string) => void;
  uploadFn: (file: File, onProgress?: (percent: number) => void) => Promise<{ url: string }>;
  shape?: "square" | "circle";
}

export function ImageUploadField({ label, value, onChange, uploadFn, shape = "square" }: ImageUploadFieldProps) {
  const galleryInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const [progress, setProgress] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [pendingImageSrc, setPendingImageSrc] = useState<string | null>(null);
  const [viewerOpen, setViewerOpen] = useState(false);

  useEffect(() => {
    if (!menuOpen) return;
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false);
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [menuOpen]);

  const upload = async (file: File) => {
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

  const handlePicked = (file: File) => {
    setMenuOpen(false);
    setPendingImageSrc(URL.createObjectURL(file));
  };

  const handleCropped = (blob: Blob) => {
    if (pendingImageSrc) URL.revokeObjectURL(pendingImageSrc);
    setPendingImageSrc(null);
    upload(new File([blob], "photo.jpg", { type: "image/jpeg" }));
  };

  const cancelCrop = () => {
    if (pendingImageSrc) URL.revokeObjectURL(pendingImageSrc);
    setPendingImageSrc(null);
  };

  return (
    <div className="flex flex-col gap-2">
      <span className="text-sm font-medium text-ink-700">{label}</span>
      <div ref={menuRef} className="relative w-fit">
        <button
          type="button"
          onClick={() => setMenuOpen((v) => !v)}
          onDragOver={(e) => e.preventDefault()}
          onDrop={(e) => {
            e.preventDefault();
            const file = e.dataTransfer.files?.[0];
            if (file) handlePicked(file);
          }}
          className={clsx(
            "focus-ring flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden border-2 border-dashed border-ink-200 bg-paper-100 transition-colors hover:border-signal-300 hover:bg-signal-50/40",
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
        </button>

        {menuOpen && (
          <div className="absolute left-0 top-full z-20 mt-2 w-52 overflow-hidden rounded-xl border border-surface-border bg-surface shadow-lg">
            <button
              type="button"
              onClick={() => cameraInputRef.current?.click()}
              className="flex w-full items-center gap-2.5 px-3.5 py-2.5 text-left text-sm text-ink-700 hover:bg-surface-hover"
            >
              <CameraIcon className="h-4 w-4 text-ink-400" />
              Take photo
            </button>
            <button
              type="button"
              onClick={() => galleryInputRef.current?.click()}
              className="flex w-full items-center gap-2.5 px-3.5 py-2.5 text-left text-sm text-ink-700 hover:bg-surface-hover"
            >
              <ImageStackIcon className="h-4 w-4 text-ink-400" />
              Choose from gallery
            </button>
            {value && (
              <>
                <button
                  type="button"
                  onClick={() => {
                    setMenuOpen(false);
                    setViewerOpen(true);
                  }}
                  className="flex w-full items-center gap-2.5 px-3.5 py-2.5 text-left text-sm text-ink-700 hover:bg-surface-hover"
                >
                  <EyeIcon className="h-4 w-4 text-ink-400" />
                  View photo
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setMenuOpen(false);
                    onChange("");
                  }}
                  className="flex w-full items-center gap-2.5 border-t border-surface-border px-3.5 py-2.5 text-left text-sm text-danger-500 hover:bg-danger-soft"
                >
                  <TrashIcon className="h-4 w-4" />
                  Remove photo
                </button>
              </>
            )}
          </div>
        )}
      </div>

      <input
        ref={galleryInputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0];
          e.target.value = "";
          if (file) handlePicked(file);
        }}
      />
      {/* capture="environment" is what actually routes mobile browsers straight to the camera app
          instead of the gallery - the plain input above has no capture attribute so it opens the
          normal file/photo picker. Desktop browsers ignore capture and just open the file browser. */}
      <input
        ref={cameraInputRef}
        type="file"
        accept="image/*"
        capture="environment"
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0];
          e.target.value = "";
          if (file) handlePicked(file);
        }}
      />

      {error && <span className="text-xs font-medium text-danger-500">{error}</span>}

      {pendingImageSrc && (
        <AvatarCropModal
          imageSrc={pendingImageSrc}
          roundMask={shape === "circle"}
          onCancel={cancelCrop}
          onCropped={handleCropped}
        />
      )}
      {viewerOpen && value && <ImageViewerModal imageUrl={value} alt={label} onClose={() => setViewerOpen(false)} />}
    </div>
  );
}
