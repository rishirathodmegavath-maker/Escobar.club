import { useEffect, useRef, useState } from "react";
import { draftsApi } from "@/api/drafts";
import { useDebouncedValue } from "./useDebouncedValue";

export type DraftSaveStatus = "idle" | "saving" | "saved" | "error";

/**
 * Debounce-saves `value` to the server as the user types, so a half-filled form survives a
 * crashed tab or a network drop. `enabled` should stay false until the form has loaded its real
 * data and the user has actually touched it - otherwise every visit creates a draft.
 */
export function useDraftAutosave<T>(draftKey: string, value: T, enabled: boolean, debounceMs = 1500): DraftSaveStatus {
  const [status, setStatus] = useState<DraftSaveStatus>("idle");
  const debouncedValue = useDebouncedValue(value, debounceMs);
  const latestValue = useRef(value);
  latestValue.current = value;

  useEffect(() => {
    if (!enabled) return;
    let cancelled = false;
    setStatus("saving");
    draftsApi
      .save(draftKey, debouncedValue)
      .then(() => !cancelled && setStatus("saved"))
      .catch(() => !cancelled && setStatus("error"));
    return () => {
      cancelled = true;
    };
  }, [draftKey, debouncedValue, enabled]);

  // Best-effort last-chance save when the tab is hidden/closed, since the debounce above may not
  // have fired yet for the last few keystrokes.
  useEffect(() => {
    if (!enabled) return;
    const flush = () => {
      draftsApi.save(draftKey, latestValue.current).catch(() => {});
    };
    const onVisibilityChange = () => {
      if (document.visibilityState === "hidden") flush();
    };
    document.addEventListener("visibilitychange", onVisibilityChange);
    window.addEventListener("pagehide", flush);
    return () => {
      document.removeEventListener("visibilitychange", onVisibilityChange);
      window.removeEventListener("pagehide", flush);
    };
  }, [draftKey, enabled]);

  return status;
}

/** Fetches a previously saved draft once, for forms to offer as a "restore your unsaved work?" prompt. */
export function useDraftRestore<T>(draftKey: string, enabled = true) {
  const [draft, setDraft] = useState<{ data: T; updatedAt: string } | null>(null);
  const [isLoading, setIsLoading] = useState(enabled);

  useEffect(() => {
    if (!enabled) {
      setIsLoading(false);
      return;
    }
    let cancelled = false;
    setIsLoading(true);
    draftsApi
      .get<T>(draftKey)
      .then((result) => !cancelled && setDraft(result))
      .finally(() => !cancelled && setIsLoading(false));
    return () => {
      cancelled = true;
    };
  }, [draftKey, enabled]);

  const dismiss = () => setDraft(null);

  return { draft, isLoading, dismiss };
}
