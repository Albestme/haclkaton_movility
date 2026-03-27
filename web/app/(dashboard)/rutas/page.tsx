import { initialTechnicians, initialWorkOrders } from "@/src/features/operations/data";
import { GeoPoint, TechnicianStatus } from "@/src/features/operations/types";
import type { TechnicianRoute } from "@/src/components/technicians-routes-map";
import TechniciansRoutesMapClient from "@/src/components/technicians-routes-map-client";

const orderDestinationById: Record<string, GeoPoint> = {
  "ot-2026-104": { lat: 41.3936, lng: 2.1937 },
  "ot-2026-105": { lat: 41.1184, lng: 1.2451 },
  "ot-2026-106": { lat: 41.1564, lng: 1.1079 },
  "ot-2026-107": { lat: 41.9972, lng: 2.8093 },
};

const statusLabel: Record<TechnicianStatus, string> = {
  available: "Disponible",
  on_route: "En ruta",
  working: "Trabajando",
  offline: "Fuera de línea",
};

function buildRoutePoints(start: GeoPoint, end: GeoPoint): GeoPoint[] {
  const mid: GeoPoint = {
    lat: (start.lat + end.lat) / 2 + 0.02,
    lng: (start.lng + end.lng) / 2 - 0.015,
  };

  return [start, mid, end];
}

function getAssignedRoutes(): TechnicianRoute[] {
  return initialTechnicians
    .filter((technician) => technician.activeOrderId)
    .map((technician) => {
      const order = initialWorkOrders.find((item) => item.id === technician.activeOrderId);
      const destination = order ? orderDestinationById[order.id] : undefined;

      if (!order || !destination) {
        return null;
      }

      return {
        technicianId: technician.id,
        technicianName: technician.name,
        status: technician.status,
        orderId: order.id,
        orderSiteName: order.siteName,
        path: buildRoutePoints(technician.location, destination),
      };
    })
    .filter((route): route is TechnicianRoute => route !== null);
}

export default function RutasPage() {
  const routes = getAssignedRoutes();

  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-8 md:px-6">
      <header className="mb-6 rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-sky-700">Plan de rutas</p>
        <h1 className="mt-2 text-2xl font-bold md:text-3xl">Rutas asignadas de técnicos</h1>
        <p className="mt-2 text-sm text-slate-600 md:text-base">
          Visualiza en OpenStreetMap el recorrido asignado a cada trabajador con orden activa.
        </p>
      </header>

      <section className="mb-6 rounded-2xl bg-white p-5 shadow-sm">
        <h2 className="mb-3 text-lg font-semibold">Mapa de rutas (OpenStreetMap)</h2>
        {routes.length > 0 ? (
          <TechniciansRoutesMapClient routes={routes} />
        ) : (
          <p className="text-sm text-slate-500">No hay rutas activas en este momento.</p>
        )}
      </section>

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <header className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
          <h2 className="text-sm font-semibold text-slate-800">Detalle de rutas asignadas</h2>
          <span className="text-xs text-slate-500">{routes.length} rutas</span>
        </header>

        {routes.length === 0 ? (
          <p className="px-4 py-6 text-sm text-slate-500">Sin técnicos con orden activa.</p>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
              <tr>
                <th className="px-4 py-3">Técnico</th>
                <th className="px-4 py-3">OT</th>
                <th className="px-4 py-3">Destino</th>
                <th className="px-4 py-3">Estado</th>
              </tr>
            </thead>
            <tbody>
              {routes.map((route) => (
                <tr key={`${route.technicianId}-${route.orderId}`} className="border-t border-slate-200">
                  <td className="px-4 py-3 font-medium">{route.technicianName}</td>
                  <td className="px-4 py-3">{route.orderId}</td>
                  <td className="px-4 py-3">{route.orderSiteName}</td>
                  <td className="px-4 py-3">{statusLabel[route.status]}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </main>
  );
}

