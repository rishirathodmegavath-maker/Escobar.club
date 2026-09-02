// A URL typed without a scheme (e.g. "www.example.com" or "example.com/page") is something people
// type constantly and shouldn't be rejected outright - we accept anything domain-shaped and
// normalize it to https:// before it's stored, since these values later get used as raw <a href>
// targets. Dangerous schemes like javascript:/data:/vbscript: are never domain-shaped, so this
// pattern rejects them by construction rather than needing an explicit denylist.
const DOMAIN_LIKE_PATTERN =
  /^(?:https?:\/\/)?[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+(?::\d+)?(?:\/\S*)?$/i;

export function isUrlLike(value: string): boolean {
  return DOMAIN_LIKE_PATTERN.test(value.trim());
}

export function normalizeUrl(value: string): string {
  const trimmed = value.trim();
  if (!trimmed || /^https?:\/\//i.test(trimmed)) return trimmed;
  return `https://${trimmed}`;
}
