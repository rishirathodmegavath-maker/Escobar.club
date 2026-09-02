const HEIC_EXTENSION_PATTERN = /\.hei[cf]$/i;

function looksLikeHeic(file: File): boolean {
  const type = file.type.toLowerCase();
  return type === "image/heic" || type === "image/heif" || HEIC_EXTENSION_PATTERN.test(file.name);
}

/**
 * iPhones save camera photos as HEIC by default, and browsers can't decode that format through
 * <img>/canvas - the crop step would show a blank/black preview and fail to export. Convert to
 * JPEG client-side first (lazy-loaded, since most picked files never need this) so the rest of the
 * upload flow never has to know the difference.
 */
export async function ensureBrowserRenderableImage(file: File): Promise<File> {
  if (!looksLikeHeic(file)) return file;
  const heic2any = (await import("heic2any")).default;
  const result = await heic2any({ blob: file, toType: "image/jpeg", quality: 0.9 });
  const blob = Array.isArray(result) ? result[0] : result;
  return new File([blob], file.name.replace(HEIC_EXTENSION_PATTERN, ".jpg"), { type: "image/jpeg" });
}
