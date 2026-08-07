import type { ReactNode } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import clsx from "clsx";
import { useAuth } from "@/auth/AuthContext";
import { businessesApi } from "@/api/businesses";
import { creatorsApi } from "@/api/creators";
import { Avatar } from "./Avatar";
import { Button } from "./Button";
import { SignalMark } from "./SignalMark";
import { CompassIcon, ImageStackIcon, UserIcon, LogoutIcon, TrophyIcon, MegaphoneIcon, IdCardIcon, ShieldIcon, SparkIcon, EyeIcon } from "./icons";

interface NavItem {
  to: string;
  label: string;
  icon: (props: { className?: string }) => JSX.Element;
}

function navItemsForRole(role: string | undefined): NavItem[] {
  if (role === "BUSINESS") {
    return [
      { to: "/", label: "Discover", icon: CompassIcon },
      { to: "/business/campaigns", label: "My campaigns", icon: MegaphoneIcon },
      { to: "/business/content", label: "Review queue", icon: ImageStackIcon },
      { to: "/business/leaderboard", label: "Leaderboard", icon: TrophyIcon },
      { to: "/business/profile", label: "Company profile", icon: UserIcon },
      { to: "/account/security", label: "Security", icon: ShieldIcon },
    ];
  }
  if (role === "CREATOR") {
    return [
      { to: "/", label: "Discover", icon: CompassIcon },
      { to: "/creator/content", label: "My submissions", icon: ImageStackIcon },
      { to: "/leaderboard", label: "Leaderboard", icon: TrophyIcon },
      { to: "/creator/kyc", label: "My KYC", icon: IdCardIcon },
      { to: "/creator/profile", label: "My profile", icon: UserIcon },
      { to: "/account/security", label: "Security", icon: ShieldIcon },
    ];
  }
  if (role === "ADMIN") {
    return [
      { to: "/admin", label: "Dashboard", icon: SparkIcon },
      { to: "/admin/creators", label: "Creators", icon: UserIcon },
      { to: "/admin/businesses", label: "Brands", icon: MegaphoneIcon },
      { to: "/admin/campaigns", label: "Campaigns", icon: CompassIcon },
      { to: "/admin/content", label: "Content & metrics", icon: EyeIcon },
    ];
  }
  return [{ to: "/", label: "Discover", icon: CompassIcon }];
}

export function AppShell({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const items = navItemsForRole(user?.role);

  const { data: businessProfile } = useQuery({
    queryKey: ["business", "me"],
    queryFn: businessesApi.getMine,
    enabled: user?.role === "BUSINESS",
  });
  const { data: creatorProfile } = useQuery({
    queryKey: ["creator", "me"],
    queryFn: creatorsApi.getMine,
    enabled: user?.role === "CREATOR",
  });
  const avatarImageUrl = businessProfile?.logoUrl ?? creatorProfile?.profilePictureUrl;

  return (
    <div className="flex min-h-screen bg-paper">
      <aside className="sticky top-0 flex h-screen w-64 shrink-0 flex-col bg-ink-900 px-5 py-6">
        <div className="mb-8 flex items-center gap-2.5 px-2">
          <SignalMark size={30} />
          <span className="font-display text-lg font-semibold tracking-tight text-paper-50">Escobar.Club</span>
        </div>

        <nav className="flex flex-1 flex-col gap-1">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === "/" || item.to === "/admin"}
              className={({ isActive }) =>
                clsx(
                  "focus-ring flex items-center gap-3 rounded-lg border-l-2 px-3 py-2.5 text-sm font-medium transition-colors",
                  isActive
                    ? "border-signal-500 bg-white/10 text-paper-50"
                    : "border-transparent text-paper-50/60 hover:bg-white/5 hover:text-paper-50",
                )
              }
            >
              <item.icon className="h-[18px] w-[18px]" />
              {item.label}
            </NavLink>
          ))}
        </nav>

        {user ? (
          <div className="mt-4 flex items-center gap-3 rounded-xl border border-white/10 bg-white/5 px-3 py-3">
            <Avatar name={user.email} imageUrl={avatarImageUrl} size={34} />
            <div className="min-w-0 flex-1">
              <p className="truncate text-xs font-semibold text-paper-50">{user.email}</p>
              <p className="text-[11px] uppercase tracking-wide text-paper-50/50">{user.role.toLowerCase()}</p>
            </div>
            <button
              aria-label="Log out"
              onClick={() => {
                logout();
                navigate("/login");
              }}
              className="focus-ring rounded-md p-1.5 text-paper-50/50 hover:bg-white/10 hover:text-danger-400"
            >
              <LogoutIcon className="h-4 w-4" />
            </button>
          </div>
        ) : (
          <div className="mt-4 flex flex-col gap-2 rounded-xl border border-white/10 bg-white/5 px-3 py-3">
            <p className="px-1 text-xs text-paper-50/60">Sign in to apply, review, and manage partnerships.</p>
            <Link to="/login">
              <Button size="sm" className="w-full">
                Sign in
              </Button>
            </Link>
            <Link to="/register">
              <Button size="sm" variant="secondary" className="w-full">
                Create account
              </Button>
            </Link>
          </div>
        )}
      </aside>

      <main className="min-w-0 flex-1 px-8 py-8 lg:px-12">
        <div className="mx-auto max-w-6xl animate-fade-in">{children}</div>
      </main>
    </div>
  );
}
