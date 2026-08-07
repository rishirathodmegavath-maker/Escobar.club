import { useEffect, useState } from "react";
import clsx from "clsx";

function initialsFrom(name: string) {
  const parts = name.trim().split(/\s+/);
  return parts
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase())
    .join("");
}

const palette = [
  "bg-mint-soft text-mint-deep",
  "bg-gold-soft text-gold-deep",
  "bg-alert-soft text-alert-deep",
  "bg-danger-soft text-danger-deep",
];

interface AvatarProps {
  name: string;
  imageUrl?: string | null;
  size?: number;
  className?: string;
}

export function Avatar({ name, imageUrl, size = 40, className }: AvatarProps) {
  const paletteIndex = name.length % palette.length;
  const [imageFailed, setImageFailed] = useState(false);
  useEffect(() => setImageFailed(false), [imageUrl]);

  if (imageUrl && !imageFailed) {
    return (
      <img
        src={imageUrl}
        alt=""
        onError={() => setImageFailed(true)}
        className={clsx("shrink-0 rounded-avatar object-cover", className)}
        style={{ width: size, height: size }}
      />
    );
  }

  return (
    <span
      className={clsx("flex shrink-0 items-center justify-center rounded-avatar font-display font-semibold", palette[paletteIndex], className)}
      style={{ width: size, height: size, fontSize: size * 0.4 }}
    >
      {initialsFrom(name) || "?"}
    </span>
  );
}
