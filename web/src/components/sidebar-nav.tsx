"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

type NavItem = {
  href: string;
  label: string;
  shortLabel: string;
};

const navItems: NavItem[] = [
  { href: "/panel", label: "Panel", shortLabel: "PL" },
  { href: "/control", label: "Control", shortLabel: "CT" },
  { href: "/tareas", label: "Tareas", shortLabel: "TS" },
  { href: "/tecnicos", label: "Tecnicos", shortLabel: "TC" },
];

type SidebarNavProps = {
  isSidebarOpen: boolean;
  onToggleSidebar: () => void;
};

export default function SidebarNav({ isSidebarOpen, onToggleSidebar }: SidebarNavProps) {
  const pathname = usePathname();

  return (
    <aside
      className={`h-screen shrink-0 border-r border-zinc-800 bg-zinc-950 text-zinc-100 transition-all ${
        isSidebarOpen ? "w-20 md:w-64" : "w-20"
      }`}
    >
      <div className="flex h-full flex-col px-2 py-5 md:px-3">
        <div className="mb-6 px-1 md:px-3">
          <p className={`text-xs uppercase tracking-[0.2em] text-zinc-400 ${isSidebarOpen ? "hidden md:block" : "hidden"}`}>
            Operary EV
          </p>
          <button
            type="button"
            onClick={onToggleSidebar}
            aria-label={isSidebarOpen ? "Ocultar menu lateral" : "Mostrar menu lateral"}
            aria-expanded={isSidebarOpen}
            className="mt-3 inline-flex h-10 w-10 items-center justify-center rounded-lg bg-zinc-800 text-white hover:bg-zinc-700"
          >
            {isSidebarOpen ? (
              <svg
                aria-hidden="true"
                viewBox="0 0 24 24"
                className="h-5 w-5"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M18 6 6 18" />
                <path d="m6 6 12 12" />
              </svg>
            ) : (
              <svg
                aria-hidden="true"
                viewBox="0 0 24 24"
                className="h-5 w-5"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M4 6h16" />
                <path d="M4 12h16" />
                <path d="M4 18h16" />
              </svg>
            )}
          </button>
        </div>

        <nav className={`space-y-1 ${isSidebarOpen ? "" : "hidden"}`}>
          {navItems.map((item) => {
            const isActive = pathname === item.href;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-3 rounded-xl px-3 py-2 text-sm transition ${
                  isActive
                    ? "bg-zinc-100 text-zinc-950"
                    : "text-zinc-300 hover:bg-zinc-900 hover:text-white"
                }`}
              >
                <span
                  className={`inline-flex h-7 w-7 items-center justify-center rounded-lg text-[11px] font-semibold ${
                    isActive ? "bg-zinc-950 text-zinc-100" : "bg-zinc-800 text-zinc-200"
                  }`}
                >
                  {item.shortLabel}
                </span>
                <span className="hidden md:block">{item.label}</span>
              </Link>
            );
          })}
        </nav>
      </div>
    </aside>
  );
}

