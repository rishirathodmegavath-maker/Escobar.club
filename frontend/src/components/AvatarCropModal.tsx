import { useCallback, useState } from "react";
import Cropper, { type Area } from "react-easy-crop";
import { Button } from "@/components/Button";
import { XIcon } from "@/components/icons";
import { cropImageToBlob } from "@/lib/imageCrop";
import { extractErrorMessage } from "@/api/client";

export function AvatarCropModal({
  imageSrc,
  roundMask = true,
  onCancel,
  onCropped,
}: {
  imageSrc: string;
  /** Purely a visual guide while cropping - the exported image is always a plain square either way. */
  roundMask?: boolean;
  onCancel: () => void;
  onCropped: (blob: Blob) => void;
}) {
  const [crop, setCrop] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [croppedAreaPixels, setCroppedAreaPixels] = useState<Area | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const onCropComplete = useCallback((_: Area, pixels: Area) => setCroppedAreaPixels(pixels), []);

  const handleSave = async () => {
    if (!croppedAreaPixels) return;
    setSaving(true);
    setError(null);
    try {
      const blob = await cropImageToBlob(imageSrc, croppedAreaPixels);
      onCropped(blob);
    } catch (err) {
      setError(extractErrorMessage(err, "Could not crop this image"));
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/70 p-4">
      <div className="card-surface flex w-full max-w-md flex-col gap-4 p-5">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-lg font-semibold text-ink-900">Crop photo</h2>
          <button
            type="button"
            onClick={onCancel}
            className="focus-ring rounded-full p-1 text-ink-400 hover:bg-surface-hover hover:text-ink-700"
            aria-label="Close"
          >
            <XIcon className="h-5 w-5" />
          </button>
        </div>

        <div className="relative h-72 w-full overflow-hidden rounded-xl bg-ink-900">
          <Cropper
            image={imageSrc}
            crop={crop}
            zoom={zoom}
            aspect={1}
            cropShape={roundMask ? "round" : "rect"}
            showGrid={!roundMask}
            onCropChange={setCrop}
            onZoomChange={setZoom}
            onCropComplete={onCropComplete}
          />
        </div>

        <label className="flex flex-col gap-1.5">
          <span className="text-xs font-medium text-ink-500">Zoom</span>
          <input
            type="range"
            min={1}
            max={3}
            step={0.05}
            value={zoom}
            onChange={(e) => setZoom(Number(e.target.value))}
            className="w-full accent-signal-500"
          />
        </label>

        {error && <span className="text-xs font-medium text-danger-500">{error}</span>}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="ghost" onClick={onCancel} disabled={saving}>
            Cancel
          </Button>
          <Button type="button" onClick={handleSave} isLoading={saving} disabled={!croppedAreaPixels}>
            Save
          </Button>
        </div>
      </div>
    </div>
  );
}
