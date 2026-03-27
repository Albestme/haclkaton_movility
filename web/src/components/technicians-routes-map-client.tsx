"use client";

import dynamic from "next/dynamic";
import type { TechnicianRoute } from "@/src/components/technicians-routes-map";

const TechniciansRoutesMap = dynamic(() => import("@/src/components/technicians-routes-map"), {
  ssr: false,
  loading: () => (
    <div className="flex h-90 items-center justify-center rounded-xl border border-slate-200 bg-slate-50 text-sm text-slate-500">
      Cargando rutas...
    </div>
  ),
});

type TechniciansRoutesMapClientProps = {
  routes: TechnicianRoute[];
};

export default function TechniciansRoutesMapClient({ routes }: TechniciansRoutesMapClientProps) {
  return <TechniciansRoutesMap routes={routes} />;
}

