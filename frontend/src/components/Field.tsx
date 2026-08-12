import { forwardRef, useState, type InputHTMLAttributes, type TextareaHTMLAttributes } from "react";
import clsx from "clsx";
import { EyeIcon, EyeOffIcon } from "./icons";

interface FieldWrapperProps {
  label?: string;
  error?: string;
  hint?: string;
  children: React.ReactNode;
}

export function FieldWrapper({ label, error, hint, children }: FieldWrapperProps) {
  return (
    <label className="flex flex-col gap-1.5">
      {label && <span className="text-sm font-medium text-ink-700">{label}</span>}
      {children}
      {hint && !error && <span className="text-xs text-ink-400">{hint}</span>}
      {error && <span className="text-xs font-medium text-danger-500">{error}</span>}
    </label>
  );
}

const baseInputClasses =
  "focus-ring w-full rounded-lg border border-ink-200 bg-surface-input px-3.5 py-2.5 text-sm text-ink-900 placeholder:text-ink-300 transition-colors focus:border-signal-500";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(({ label, error, hint, className, type, ...props }, ref) => {
  const [reveal, setReveal] = useState(false);
  const isPassword = type === "password";

  return (
    <FieldWrapper label={label} error={error} hint={hint}>
      {isPassword ? (
        <div className="relative">
          <input
            ref={ref}
            type={reveal ? "text" : "password"}
            className={clsx(baseInputClasses, "pr-10", error && "border-danger-300 focus-visible:ring-danger-300", className)}
            {...props}
          />
          <button
            type="button"
            tabIndex={-1}
            onClick={() => setReveal((v) => !v)}
            aria-label={reveal ? "Hide password" : "Show password"}
            className="focus-ring absolute right-2.5 top-1/2 -translate-y-1/2 rounded p-1 text-ink-400 hover:text-ink-600"
          >
            {reveal ? <EyeOffIcon width={16} height={16} /> : <EyeIcon width={16} height={16} />}
          </button>
        </div>
      ) : (
        <input
          ref={ref}
          type={type}
          className={clsx(baseInputClasses, error && "border-danger-300 focus-visible:ring-danger-300", className)}
          {...props}
        />
      )}
    </FieldWrapper>
  );
});
Input.displayName = "Input";

interface TextAreaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export const TextArea = forwardRef<HTMLTextAreaElement, TextAreaProps>(
  ({ label, error, hint, className, rows = 4, ...props }, ref) => (
    <FieldWrapper label={label} error={error} hint={hint}>
      <textarea
        ref={ref}
        rows={rows}
        className={clsx(baseInputClasses, "resize-none", error && "border-danger-300 focus-visible:ring-danger-300", className)}
        {...props}
      />
    </FieldWrapper>
  ),
);
TextArea.displayName = "TextArea";
