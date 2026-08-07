import clsx from "clsx";

interface SignalMarkProps {
  /** Overall box size in pixels. The core dot and rings scale off this. */
  size?: number;
  /** Plays the ignite-in and continuous broadcast rings. Off = static dot only. */
  animated?: boolean;
  /** Fires one extra "settled" pulse shortly after ignite - splash/hero use only. */
  chime?: boolean;
  /** Delay (ms) before the ignite/ring animation starts, for staging in a larger sequence. */
  igniteDelay?: number;
  className?: string;
}

/**
 * Escobar.Club's mark: a single signal broadcasting outward. Same glyph used
 * standalone (nav, favicon) and as the hero of SplashScreen - see that file
 * for the full reveal choreography this is built to be staged inside.
 */
export function SignalMark({ size = 40, animated = true, chime = false, igniteDelay = 0, className }: SignalMarkProps) {
  const delay = (extraMs: number) => `${(igniteDelay + extraMs) / 1000}s`;

  return (
    <span
      className={clsx("relative inline-flex shrink-0 items-center justify-center", className)}
      style={{ width: size, height: size }}
      aria-hidden="true"
    >
      {animated && (
        <>
          <span
            className="absolute inset-0 rounded-full border border-signal-400 animate-signal-ring motion-reduce:hidden"
            style={{ animationDelay: delay(0) }}
          />
          <span
            className="absolute inset-0 rounded-full border border-signal-400 animate-signal-ring motion-reduce:hidden"
            style={{ animationDelay: delay(700) }}
          />
        </>
      )}
      {chime && (
        <span
          className="absolute inset-0 rounded-full border border-signal-300 opacity-0 animate-signal-chime motion-reduce:hidden"
          style={{ animationDelay: delay(1300) }}
        />
      )}
      <span
        className={clsx(
          "relative rounded-full bg-signal-500",
          animated && "animate-signal-ignite motion-reduce:animate-none",
        )}
        style={{ width: size * 0.34, height: size * 0.34, animationDelay: delay(0) }}
      />
    </span>
  );
}
