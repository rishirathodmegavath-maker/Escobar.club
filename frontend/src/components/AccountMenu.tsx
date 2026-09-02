import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Link } from "react-router-dom";
import clsx from "clsx";
import { Avatar } from "./Avatar";
import { ChevronRightIcon, LogoutIcon } from "./icons";

export interface AccountMenuItem {
  to: string;
  label: string;
  icon: (props: { className?: string }) => JSX.Element;
}

interface MenuPosition {
  left: number;
  bottom: number;
  width: number;
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
  const [position, setPosition] = useState<MenuPosition | null>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  // The trigger can sit inside a scrollable, collapsible, or mobile-overlay sidebar - measuring its
  // real position and rendering the menu into a body-level portal (fixed coords) avoids it ever being
  // clipped by an ancestor's overflow, regardless of which of those layouts is currently active.
  useEffect(() => {
    if (!open) return;
    const trigger = triggerRef.current;
    if (trigger) {
      const rect = trigger.getBoundingClientRect();
      const width = Math.max(rect.width, 256);
      setPosition({
        left: Math.min(rect.left, window.innerWidth - width - 12),
        bottom: window.innerHeight - rect.top + 8,
        width,
      });
    }

    const handlePointerDown = (e: MouseEvent) => {
      const target = e.target as Node;
      if (triggerRef.current?.contains(target) || menuRef.current?.contains(target)) return;
      setOpen(false);
    };
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    const closeOnScroll = () => setOpen(false);
    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    window.addEventListener("scroll", closeOnScroll, true);
    window.addEventListener("resize", closeOnScroll);
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("scroll", closeOnScroll, true);
      window.removeEventListener("resize", closeOnScroll);
    };
  }, [open]);

  return (
    <>
      {open &&
        position &&
        createPortal(
          <div
            ref={menuRef}
            role="menu"
            style={{ position: "fixed", left: position.left, bottom: position.bottom, width: position.width }}
            className="z-[60] overflow-hidden rounded-xl border border-white/10 bg-[#18140F] py-1.5 shadow-pop"
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
          </div>,
          document.body,
        )}

      <button
        ref={triggerRef}
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
        className={clsx(
          "focus-ring mt-4 flex w-full items-center rounded-xl border border-white/10 bg-white/5 py-3 text-left transition-colors hover:bg-white/10",
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
    </>
  );
}
