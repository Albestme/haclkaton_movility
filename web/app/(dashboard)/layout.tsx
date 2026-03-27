"use client";

import { useState } from "react";
import SidebarNav from "@/src/components/sidebar-nav";

export default function DashboardLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  return (
    <div className="flex min-h-screen bg-slate-100">
      <SidebarNav
        isSidebarOpen={isSidebarOpen}
        onToggleSidebar={() => setIsSidebarOpen((current) => !current)}
      />
      <div className="flex min-h-screen flex-1 flex-col">
        <main className="flex-1">{children}</main>
      </div>
    </div>
  );
}

