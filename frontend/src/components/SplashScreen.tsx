import { useEffect, useState } from "react";
import clsx from "clsx";
import { SignalMark } from "./SignalMark";

/**
 * Full reveal takes ~2.3s (ignite -> wordmark settle -> confirm chime); the
 * ambient broadcast rings keep looping after that as the "still working" idle
 * state. This floor guarantees the reveal is always seen in full even when
 * AuthProvider resolves in a single tick, so a fast network never truncates
 * the brand moment mid-animation.
 */
const MIN_DISPLAY_MS = 2600;
const EXIT_DURATION_MS = 420;

export function SplashScreen({ ready, onDone }: { ready: boolean; onDone: () => void }) {
  const [exiting, setExiting] = useState(false);
  const [mountedAt] = useState(() => Date.now());

  useEffect(() => {
    if (!ready) return;
    const remaining = Math.max(MIN_DISPLAY_MS - (Date.now() - mountedAt), 0);
    const timer = setTimeout(() => setExiting(true), remaining);
    return () => clearTimeout(timer);
  }, [ready, mountedAt]);

  useEffect(() => {
    if (!exiting) return;
    const timer = setTimeout(onDone, EXIT_DURATION_MS);
    return () => clearTimeout(timer);
  }, [exiting, onDone]);

  return (
    <div
      className={clsx(
        "fixed inset-0 z-50 flex items-center justify-center bg-ink-950 animate-splash-in",
        exiting && "animate-splash-out",
      )}
      role="status"
      aria-live="polite"
      aria-label="Loading Escobar.Club"
    >
      <div className="flex items-center gap-3">
        <span className="animate-lockup-settle" style={{ animationDelay: "0.65s" }}>
          <SignalMark size={72} igniteDelay={200} chime />
        </span>
        <span
          className="flex items-baseline font-display text-3xl font-semibold tracking-tight text-paper-50 opacity-0 animate-wordmark-in"
          style={{ animationDelay: "0.65s" }}
        >
          escobar
          <span className="mx-px inline-block h-1.5 w-1.5 -translate-y-2 rounded-full bg-signal-400" />
          club
        </span>
      </div>
    </div>
  );
}
