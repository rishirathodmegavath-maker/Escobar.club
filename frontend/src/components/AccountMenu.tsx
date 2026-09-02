import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import clsx from "clsx";
import { Avatar } from "./Avatar";
import { ChevronRightIcon, LogoutIcon } from "./icons";

export interface AccountMenuItem {
  to: string;
  label: string;
  icon: (props: { className?: string }) => JSX.Element;
}

export function AccountMenu({
  email,
  role,
  avatarImageUrl,
  collapsed,
  items,
  onLogout,
}: {
  email: string;
  role: string;
  avatarImageUrl?: string | null;
  collapsed: boolean;
  items: AccountMenuItem[];
  onLogout: () => void;
}) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const handlePointerDown = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  return (
    <div ref={containerRef} className="relative mt-4">
      {open && (
        <div
          role="menu"
          className="absolute bottom-full left-0 z-30 mb-2 w-64 overflow-hidden rounded-xl border border-white/10 bg-[#18140F] py-1.5 shadow-pop"
        >
          <div className="border-b border-white/10 px-3.5 py-2.5">
            <p className="truncate text-xs font-semibold text-paper-50">{email}</p>
            <p className="text-[11px] uppercase tracking-wide text-paper-50/50">{role.toLowerCase()}</p>
          </div>
          <div className="flex flex-col py-1">
            {items.map((item) => (
              <Link
                key={item.to}
                to={item.to}
                role="menuitem"
                onClick={() => setOpen(false)}
                className="focus-ring flex items-center gap-2.5 px-3.5 py-2 text-sm font-medium text-paper-50/80 transition-colors hover:bg-white/5 hover:text-paper-50"
              >
                <item.icon className="h-4 w-4 shrink-0" />
                {item.label}
              </Link>
            ))}
          </div>
          <div className="my-1 border-t border-white/10" />
          <button
            type="button"
            role="menuitem"
            onClick={() => {
              setOpen(false);
              onLogout();
            }}
            className="focus-ring flex w-full items-center gap-2.5 px-3.5 py-2 text-sm font-medium text-danger-400 transition-colors hover:bg-white/5"
          >
            <LogoutIcon className="h-4 w-4 shrink-0" />
            Log out
          </button>
        </div>
      )}

      <button
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
        className={clsx(
          "focus-ring flex w-full items-center rounded-xl border border-white/10 bg-white/5 py-3 text-left transition-colors hover:bg-white/10",
          collapsed ? "flex-col gap-2 px-2" : "gap-3 px-3",
        )}
      >
        <Avatar name={email} imageUrl={avatarImageUrl} size={collapsed ? 30 : 34} />
        {!collapsed && (
          <>
            <div className="min-w-0 flex-1">
              <p className="truncate text-xs font-semibold text-paper-50">{email}</p>
              <p className="text-[11px] uppercase tracking-wide text-paper-50/50">{role.toLowerCase()}</p>
            </div>
            <ChevronRightIcon
              className={clsx("h-3.5 w-3.5 shrink-0 text-paper-50/40 transition-transform", open ? "-rotate-90" : "rotate-90")}
            />
          </>
        )}
      </button>
    </div>
  );
}
