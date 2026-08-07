import {
  forwardRef,
  useRef,
  useState,
  type ButtonHTMLAttributes,
  type MouseEvent,
  type MutableRefObject,
  type PointerEvent,
} from "react";
import clsx from "clsx";

type Variant = "primary" | "secondary" | "ghost" | "danger" | "gold";
type Size = "sm" | "md" | "lg";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  isLoading?: boolean;
}

const variantClasses: Record<Variant, string> = {
  primary:
    "bg-gradient-to-br from-signal-500 to-signal-800 text-white shadow-[0_6px_20px_rgba(250,35,59,0.35)] hover:shadow-[0_14px_32px_rgba(250,35,59,0.55)] hover:from-signal-400 hover:to-signal-700 active:from-signal-600 active:to-signal-800",
  secondary: "bg-surface text-ink-900 border border-surface-border hover:bg-surface-hover hover:shadow-card",
  ghost: "bg-transparent text-ink-600 hover:bg-surface-hover hover:text-ink-900",
  danger: "bg-surface text-danger-500 border border-surface-border hover:bg-danger-soft hover:shadow-card",
  gold: "bg-gold-400 text-ink-950 shadow-[0_6px_18px_rgba(219,162,58,0.3)] hover:shadow-[0_14px_30px_rgba(219,162,58,0.5)] hover:bg-gold-500",
};

const sizeClasses: Record<Size, string> = {
  sm: "text-xs px-3 py-1.5 gap-1.5",
  md: "text-sm px-4 py-2.5 gap-2",
  lg: "text-base px-6 py-3 gap-2.5",
};

let rippleSeq = 0;

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    { variant = "primary", size = "md", isLoading, className, children, disabled, onMouseMove, onPointerDown, ...props },
    ref,
  ) => {
    const localRef = useRef<HTMLButtonElement | null>(null);
    const [ripples, setRipples] = useState<{ id: number; x: number; y: number; size: number }[]>([]);

    // Tracks the cursor so `.btn-shine`'s radial highlight in index.css can follow it.
    const handleMouseMove = (event: MouseEvent<HTMLButtonElement>) => {
      const el = localRef.current;
      if (el) {
        const rect = el.getBoundingClientRect();
        el.style.setProperty("--mx", `${((event.clientX - rect.left) / rect.width) * 100}%`);
        el.style.setProperty("--my", `${((event.clientY - rect.top) / rect.height) * 100}%`);
      }
      onMouseMove?.(event);
    };

    // Spawns a water-drop ripple from the exact press point; .btn-ripple in index.css
    // handles the expand-and-dissolve animation and removes itself from the DOM via
    // the timeout below once it's done playing.
    const handlePointerDown = (event: PointerEvent<HTMLButtonElement>) => {
      const el = localRef.current;
      if (el && !disabled && !isLoading) {
        const rect = el.getBoundingClientRect();
        const size = Math.max(rect.width, rect.height) * 2.2;
        const id = ++rippleSeq;
        setRipples((prev) => [...prev, { id, x: event.clientX - rect.left, y: event.clientY - rect.top, size }]);
        window.setTimeout(() => setRipples((prev) => prev.filter((r) => r.id !== id)), 700);
      }
      onPointerDown?.(event);
    };

    return (
      <button
        ref={(node) => {
          localRef.current = node;
          if (typeof ref === "function") ref(node);
          else if (ref) (ref as MutableRefObject<HTMLButtonElement | null>).current = node;
        }}
        disabled={disabled || isLoading}
        onMouseMove={handleMouseMove}
        onPointerDown={handlePointerDown}
        className={clsx(
          "btn-shine isolate focus-ring relative inline-flex items-center justify-center overflow-hidden rounded-[10px] font-medium hover:-translate-y-0.5 active:translate-y-0 active:scale-[0.96] disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0 disabled:active:scale-100",
          variantClasses[variant],
          sizeClasses[size],
          className,
        )}
        {...props}
      >
        {ripples.map((r) => (
          <span key={r.id} className="btn-ripple" style={{ left: r.x, top: r.y, width: r.size, height: r.size }} />
        ))}
        {isLoading && (
          <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-current border-t-transparent" />
        )}
        {children}
      </button>
    );
  },
);
Button.displayName = "Button";
