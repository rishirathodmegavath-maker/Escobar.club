import { useEffect, useState, type ReactNode } from "react";
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import clsx from "clsx";
import { useAuth } from "@/auth/AuthContext";
import { useTheme } from "@/theme/ThemeContext";
import { businessesApi } from "@/api/businesses";
import { creatorsApi } from "@/api/creators";
import { Button } from "./Button";
import { SignalMark } from "./SignalMark";
import { AccountMenu, type AccountMenuItem } from "./AccountMenu";
import {
  CompassIcon,
  ImageStackIcon,
  UserIcon,
  TrophyIcon,
  MegaphoneIcon,
  IdCardIcon,
  ShieldIcon,
  SparkIcon,
  EyeIcon,
  SunIcon,
  MoonIcon,
  ChevronRightIcon,
  CoinIcon,
  EditIcon,
  MenuIcon,
  XIcon,
} from "./icons";

const SIDEBAR_COLLAPSED_KEY = "escobar.sidebarCollapsed";

interface NavItem {
  to: string;
  label: string;
  icon: (props: { className?: string }) => JSX.Element;
}

interface NavSection {
  label: string;
  items: NavItem[];
}

function navSectionsForRole(role: string | undefined): NavSection[] {
  if (role === "BUSINESS") {
    return [
      {
        label: "Overview",
        items: [{ to: "/business/dashboard", label: "Dashboard", icon: SparkIcon }],
      },
      {
        label: "Browse",
        items: [
          { to: "/", label: "Discover", icon: CompassIcon },
          { to: "/business/leaderboard", label: "Leaderboard", icon: TrophyIcon },
        ],
      },
      {
        label: "Manage",
        items: [
          { to: "/business/campaigns", label: "My campaigns", icon: MegaphoneIcon },
          { to: "/business/content", label: "Review queue", icon: ImageStackIcon },
          { to: "/business/payouts", label: "Payouts", icon: CoinIcon },
          { to: "/business/profile", label: "Company profile", icon: UserIcon },
          { to: "/account/security", label: "Security", icon: ShieldIcon },
        ],
      },
    ];
  }
  if (role === "CREATOR") {
    return [
      {
        label: "Browse",
        items: [
          { to: "/", label: "Discover", icon: CompassIcon },
          { to: "/leaderboard", label: "Leaderboard", icon: TrophyIcon },
        ],
      },
      {
        label: "You",
        items: [
          { to: "/creator/content", label: "My submissions", icon: ImageStackIcon },
          { to: "/creator/kyc", label: "My KYC", icon: IdCardIcon },
          { to: "/creator/profile", label: "My profile", icon: UserIcon },
          { to: "/account/security", label: "Security", icon: ShieldIcon },
        ],
      },
    ];
  }
  if (role === "ADMIN") {
    return [
      {
        label: "Oversight",
        items: [
          { to: "/admin", label: "Dashboard", icon: SparkIcon },
          { to: "/admin/content", label: "Content & metrics", icon: EyeIcon },
        ],
      },
      {
        label: "Manage",
        items: [
          { to: "/admin/creators", label: "Creators", icon: UserIcon },
          { to: "/admin/businesses", label: "Brands", icon: MegaphoneIcon },
          { to: "/admin/campaigns", label: "Campaigns", icon: CompassIcon },
        ],
      },
    ];
  }
  return [{ label: "Browse", items: [{ to: "/", label: "Discover", icon: CompassIcon }] }];
}

function accountMenuItemsForRole(role: string | undefined): AccountMenuItem[] {
  if (role === "BUSINESS") {
    return [
      { to: "/business/profile", label: "View Profile", icon: UserIcon },
      { to: "/business/profile/edit", label: "Edit Profile", icon: EditIcon },
      { to: "/account/security", label: "Account Settings", icon: ShieldIcon },
    ];
  }
  if (role === "CREATOR") {
    return [
      { to: "/creator/profile", label: "View Profile", icon: UserIcon },
      { to: "/creator/profile/edit", label: "Edit Profile", icon: EditIcon },
      { to: "/account/security", label: "Account Settings", icon: ShieldIcon },
    ];
  }
  return [];
}

export function AppShell({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const sections = navSectionsForRole(user?.role);
  const [collapsed, setCollapsed] = useState(() => localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === "true");
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  // The desktop icon-rail ("collapsed") layout only makes sense on wide screens where it's a
  // deliberate space-saving choice - on mobile the sidebar is a full overlay drawer, so it should
  // always render expanded (with labels) regardless of what's saved from a prior desktop session.
  const showExpanded = mobileNavOpen || !collapsed;

  useEffect(() => {
    localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(collapsed));
  }, [collapsed]);

  useEffect(() => {
    setMobileNavOpen(false);
  }, [location.pathname]);

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
      <header className="fixed inset-x-0 top-0 z-30 flex h-14 items-center justify-between border-b border-white/10 bg-[#100E0C] px-4 lg:hidden">
        <button
          type="button"
          aria-label="Open menu"
          onClick={() => setMobileNavOpen(true)}
          className="focus-ring flex h-9 w-9 items-center justify-center rounded-lg text-paper-50/70 transition-colors hover:bg-white/10 hover:text-paper-50"
        >
          <MenuIcon className="h-5 w-5" />
        </button>
        <div className="flex items-center gap-2">
          <SignalMark size={22} />
          <span className="font-display text-base font-semibold tracking-tight text-paper-50">Escobar.Club</span>
        </div>
        <button
          type="button"
          aria-label={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
          title={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
          onClick={toggleTheme}
          className="focus-ring flex h-9 w-9 items-center justify-center rounded-lg text-paper-50/70 transition-colors hover:bg-white/10 hover:text-paper-50"
        >
          {theme === "dark" ? <SunIcon className="h-4 w-4" /> : <MoonIcon className="h-4 w-4" />}
        </button>
      </header>

      {mobileNavOpen && (
        <div
          aria-hidden="true"
          onClick={() => setMobileNavOpen(false)}
          className="fixed inset-0 z-40 bg-black/60 lg:hidden"
        />
      )}

      <aside
        className={clsx(
          "fixed inset-y-0 left-0 z-50 flex h-screen w-72 max-w-[85vw] flex-col overflow-y-auto border-r border-white/10 bg-[#100E0C] px-5 py-6 transition-transform duration-200 lg:sticky lg:top-0 lg:z-auto lg:w-64 lg:translate-x-0 lg:transition-[width]",
          mobileNavOpen ? "translate-x-0" : "-translate-x-full",
          collapsed && "lg:w-[76px] lg:px-3",
        )}
      >
        <button
          type="button"
          aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
          title={collapsed ? "Expand sidebar" : "Collapse sidebar"}
          onClick={() => setCollapsed((c) => !c)}
          className="focus-ring absolute -right-3 top-8 z-10 hidden h-6 w-6 items-center justify-center rounded-full border border-white/10 bg-[#100E0C] text-paper-50/60 shadow-md transition-colors hover:text-paper-50 lg:flex"
        >
          <ChevronRightIcon className={clsx("h-3.5 w-3.5 transition-transform", !collapsed && "rotate-180")} />
        </button>

        <button
          type="button"
          aria-label="Close menu"
          onClick={() => setMobileNavOpen(false)}
          className="focus-ring absolute right-4 top-6 flex h-8 w-8 items-center justify-center rounded-full text-paper-50/60 transition-colors hover:bg-white/10 hover:text-paper-50 lg:hidden"
        >
          <XIcon className="h-4 w-4" />
        </button>

        <div className={clsx("mb-8 flex items-center px-2", showExpanded ? "gap-2.5" : "lg:justify-center")}>
          <SignalMark size={30} />
          {showExpanded && <span className="font-display text-lg font-semibold tracking-tight text-paper-50">Escobar.Club</span>}
          {showExpanded && (
            <button
              type="button"
              aria-label={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
              title={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
              onClick={toggleTheme}
              className="focus-ring ml-auto hidden h-7 w-7 shrink-0 items-center justify-center rounded-full text-paper-50/60 transition-colors hover:bg-white/10 hover:text-paper-50 lg:flex"
            >
              {theme === "dark" ? <SunIcon className="h-4 w-4" /> : <MoonIcon className="h-4 w-4" />}
            </button>
          )}
        </div>

        {!showExpanded && (
          <button
            type="button"
            aria-label={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
            title={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
            onClick={toggleTheme}
            className="focus-ring mb-6 hidden h-7 w-7 shrink-0 items-center justify-center self-center rounded-full text-paper-50/60 transition-colors hover:bg-white/10 hover:text-paper-50 lg:flex"
          >
            {theme === "dark" ? <SunIcon className="h-4 w-4" /> : <MoonIcon className="h-4 w-4" />}
          </button>
        )}

        <nav className="flex flex-1 flex-col gap-1">
          {sections.map((section) => (
            <div key={section.label} className="mb-1">
              {showExpanded ? (
                <p className="mb-1.5 mt-3.5 px-3 text-[10.5px] font-bold uppercase tracking-wide text-paper-50/35 first:mt-0">
                  {section.label}
                </p>
              ) : (
                <div className="my-3 border-t border-white/10 first:mt-0" />
              )}
              <div className="flex flex-col gap-1">
                {section.items.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.to === "/" || item.to === "/admin"}
                    title={showExpanded ? undefined : item.label}
                    className={({ isActive }) =>
                      clsx(
                        "focus-ring flex items-center rounded-lg border-l-2 py-2.5 text-sm font-medium transition-colors",
                        showExpanded ? "gap-3 px-3" : "justify-center px-2",
                        isActive
                          ? "border-signal-500 bg-white/10 text-paper-50"
                          : "border-transparent text-paper-50/60 hover:bg-white/5 hover:text-paper-50",
                      )
                    }
                  >
                    <item.icon className="h-[18px] w-[18px] shrink-0" />
                    {showExpanded && item.label}
                  </NavLink>
                ))}
              </div>
            </div>
          ))}
        </nav>

        {user ? (
          <AccountMenu
            email={user.email}
            role={user.role}
            avatarImageUrl={avatarImageUrl}
            collapsed={!showExpanded}
            items={accountMenuItemsForRole(user.role)}
            onLogout={() => {
              logout();
              navigate("/login");
            }}
          />
        ) : showExpanded ? (
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
        ) : (
          <Link
            to="/login"
            title="Sign in"
            className="focus-ring mt-4 flex items-center justify-center rounded-xl border border-white/10 bg-white/5 py-3 text-paper-50/70 hover:text-paper-50"
          >
            <UserIcon className="h-[18px] w-[18px]" />
          </Link>
        )}
      </aside>

      <main className="min-w-0 flex-1 px-4 pb-8 pt-20 sm:px-6 lg:px-12 lg:py-8 lg:pt-8">
        <div className="mx-auto max-w-6xl animate-fade-in">
          <div className="mb-4 flex items-center justify-between">
            <button
              type="button"
              aria-label="Go back"
              title="Go back"
              onClick={() => navigate(-1)}
              className="focus-ring flex h-8 w-8 items-center justify-center rounded-lg border border-surface-border bg-surface text-ink-500 transition-colors hover:bg-surface-hover hover:text-ink-900"
            >
              <ChevronRightIcon className="h-4 w-4 rotate-180" />
            </button>
            <button
              type="button"
              aria-label="Go forward"
              title="Go forward"
              onClick={() => navigate(1)}
              className="focus-ring flex h-8 w-8 items-center justify-center rounded-lg border border-surface-border bg-surface text-ink-500 transition-colors hover:bg-surface-hover hover:text-ink-900"
            >
              <ChevronRightIcon className="h-4 w-4" />
            </button>
          </div>
          {children}
        </div>
      </main>
    </div>
  );
}
