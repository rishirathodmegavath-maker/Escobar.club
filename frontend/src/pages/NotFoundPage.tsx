import { Link } from "react-router-dom";
import { Button } from "@/components/Button";
import { CompassIcon } from "@/components/icons";

export function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-5 bg-paper px-4 text-center">
      <div className="flex h-14 w-14 items-center justify-center rounded-2xl border border-surface-border bg-surface text-ink-400">
        <CompassIcon className="h-7 w-7" />
      </div>
      <div className="flex flex-col gap-1.5">
        <h1 className="font-display text-2xl font-semibold text-ink-900">Page not found</h1>
        <p className="max-w-sm text-sm text-ink-500">
          There's nothing at this address. Double-check the link, or head back to somewhere that exists.
        </p>
      </div>
      <Link to="/">
        <Button>Back to Discover</Button>
      </Link>
    </div>
  );
}
