"use client";

import dynamic from "next/dynamic";
import { useMemo, useState } from "react";
import {
  initialTechnicians,
  initialWorkOrders,
} from "@/src/features/operations/data";
import {
  buildDashboardMetrics,
  filterOrders,
  getSuggestedTechnicians,
  nextOrderStatus,
} from "@/src/features/operations/metrics";
import {
  OrderFilters,
  Technician,
  TechnicianStatus,
  WorkOrder,
} from "@/src/features/operations/types";

type OperationsDashboardProps = {
  initialTechniciansData?: Technician[];
  initialOrdersData?: WorkOrder[];
};

const defaultFilters: OrderFilters = {
  charger: "all",
  priority: "all",
  status: "all",
  search: "",
};

const technicianStatusLabel: Record<TechnicianStatus, string> = {
  available: "Disponible",
  on_route: "En ruta",
  working: "Trabajando",
  offline: "Fuera de línea",
};

const orderStatusLabel: Record<WorkOrder["status"], string> = {
  Nova: "Nova",
  Assignada: "Assignada",
  "En curs": "En curs",
  Tancada: "Tancada",
};

const priorityColor = {
  "Reparaci\u00F3 cr\u00EDtica": "text-red-600",
  "Visita de diagnosi": "text-emerald-600",
  "Manteniment preventiu": "text-amber-600",
} as const;

const TechniciansMap = dynamic(() => import("@/src/components/technicians-map"), {
  ssr: false,
  loading: () => (
    <div className="flex h-[360px] items-center justify-center rounded-xl border border-slate-200 bg-slate-50 text-sm text-slate-500">
      Cargando mapa...
    </div>
  ),
});

export default function OperationsDashboard({
  initialTechniciansData,
  initialOrdersData,
}: OperationsDashboardProps) {
  const baseTechnicians = initialTechniciansData ?? initialTechnicians;
  const baseOrders = initialOrdersData ?? initialWorkOrders;

  const [technicians, setTechnicians] = useState<Technician[]>(baseTechnicians);
  const [orders, setOrders] = useState<WorkOrder[]>(baseOrders);
  const [filters, setFilters] = useState<OrderFilters>(defaultFilters);
  const [selectedOrderId, setSelectedOrderId] = useState<number | null>(baseOrders[0]?.incidence_id ?? null);

  const filteredOrders = useMemo(() => filterOrders(orders, filters), [orders, filters]);
  const selectedOrder = useMemo(
    () => orders.find((order) => order.incidence_id === selectedOrderId) ?? null,
    [orders, selectedOrderId],
  );

  const suggestedTechnicians = useMemo(() => {
    if (!selectedOrder) {
      return [];
    }
    return getSuggestedTechnicians(selectedOrder, technicians);
  }, [selectedOrder, technicians]);

  const metrics = useMemo(() => buildDashboardMetrics(technicians, orders), [technicians, orders]);
  const chargers = useMemo(() => ["all", ...new Set(orders.map((order) => String(order.charger_id)))], [orders]);
  const priorities = useMemo(() => ["all", ...new Set(orders.map((order) => order.priority))], [orders]);
  const statuses = useMemo(() => ["all", ...new Set(orders.map((order) => order.status))], [orders]);

  function assignOrder(orderId: number, technicianId: string) {
    setOrders((current) =>
      current.map((order) =>
        order.incidence_id === orderId
          ? {
              ...order,
              technicianId,
              status: order.status === "Nova" ? "Assignada" : order.status,
            }
          : order,
      ),
    );

    setTechnicians((current) =>
      current.map((technician) =>
        technician.id === technicianId
          ? {
              ...technician,
              status: "on_route",
              activeOrderId: orderId,
            }
          : technician,
      ),
    );
  }

  function advanceOrder(orderId: number) {
    setOrders((current) =>
      current.map((order) =>
        order.incidence_id === orderId
          ? {
              ...order,
              status: nextOrderStatus(order.status),
            }
          : order,
      ),
    );
  }

  function updateTechnicianStatus(technicianId: string, status: TechnicianStatus) {
    setTechnicians((current) =>
      current.map((technician) =>
        technician.id === technicianId
          ? {
              ...technician,
              status,
              activeOrderId: status === "offline" || status === "available" ? undefined : technician.activeOrderId,
            }
          : technician,
      ),
    );
  }

  return (
    <div className="min-h-screen bg-slate-100 text-slate-900">
      <main className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-4 py-8 md:px-6">
        <header className="flex flex-col gap-2 rounded-2xl bg-white p-6 shadow-sm">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-sky-700">
            Operary EV Network
          </p>
          <h1 className="text-2xl font-bold md:text-3xl">Centro de gestión de técnicos</h1>
          <p className="text-sm text-slate-600 md:text-base">
            Base inicial para asignar incidencias, seguir estados y priorizar mantenimiento de cargadores.
          </p>
        </header>

        <section className="grid gap-4 md:grid-cols-4">
          <MetricCard label="Incidencias abiertas" value={metrics.openOrders} />
          <MetricCard label="Incidencias en curso" value={metrics.inProgressOrders} />
          <MetricCard label="Incidencias cerradas" value={metrics.completedOrders} />
          <MetricCard label="Técnicos disponibles" value={metrics.availableTechnicians} />
        </section>

        <section className="grid gap-6 lg:grid-cols-[1.3fr_0.9fr]">
          <div className="space-y-4 rounded-2xl bg-white p-5 shadow-sm">
            <h2 className="text-lg font-semibold">Incidencias</h2>

            <div className="grid gap-3 md:grid-cols-4">
              <input
                value={filters.search}
                onChange={(event) =>
                  setFilters((current) => ({
                    ...current,
                    search: event.target.value,
                  }))
                }
                className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
                placeholder="Buscar incidencia, cargador o descripción"
              />

              <select
                value={filters.charger}
                onChange={(event) =>
                  setFilters((current) => ({
                    ...current,
                    charger: event.target.value,
                  }))
                }
                className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
              >
                {chargers.map((charger) => (
                  <option key={charger} value={charger}>
                    {charger === "all" ? "Todos los cargadores" : `Cargador ${charger}`}
                  </option>
                ))}
              </select>

              <select
                value={filters.priority}
                onChange={(event) =>
                  setFilters((current) => ({
                    ...current,
                    priority: event.target.value as OrderFilters["priority"],
                  }))
                }
                className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
              >
                <option value="all">Todas las prioridades</option>
                {priorities.filter((priority) => priority !== "all").map((priority) => (
                  <option key={priority} value={priority}>
                    {priority}
                  </option>
                ))}
              </select>

              <select
                value={filters.status}
                onChange={(event) =>
                  setFilters((current) => ({
                    ...current,
                    status: event.target.value as OrderFilters["status"],
                  }))
                }
                className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
              >
                <option value="all">Todos los estados</option>
                {statuses.filter((status) => status !== "all").map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
            </div>

            <div className="overflow-hidden rounded-xl border border-slate-200">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="px-3 py-3">Incidencia</th>
                    <th className="px-3 py-3">Cargador</th>
                    <th className="px-3 py-3">Prioridad</th>
                    <th className="px-3 py-3">Estado</th>
                    <th className="px-3 py-3">Técnico</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredOrders.map((order) => {
                    const assignedTech = technicians.find((technician) => technician.id === order.technicianId);
                    const isSelected = selectedOrderId === order.incidence_id;

                    return (
                      <tr
                        key={order.incidence_id}
                        onClick={() => setSelectedOrderId(order.incidence_id)}
                        className={`cursor-pointer border-t border-slate-200 ${
                          isSelected ? "bg-sky-50" : "hover:bg-slate-50"
                        }`}
                      >
                        <td className="px-3 py-3 font-medium">INC-{order.incidence_id}</td>
                        <td className="px-3 py-3">
                          <p>Cargador {order.charger_id}</p>
                          <p className="text-xs text-slate-500">{new Date(order.reported_at).toLocaleString("es-ES")}</p>
                        </td>
                        <td className={`px-3 py-3 font-semibold ${priorityColor[order.priority as keyof typeof priorityColor] ?? "text-slate-700"}`}>
                          {order.priority}
                        </td>
                        <td className="px-3 py-3">{orderStatusLabel[order.status]}</td>
                        <td className="px-3 py-3">{assignedTech?.name ?? "Sin asignar"}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          <aside className="space-y-4">
            <div className="rounded-2xl bg-white p-5 shadow-sm">
              <h3 className="mb-3 text-lg font-semibold">Mapa de técnicos (OpenStreetMap)</h3>
              <p className="mb-3 text-sm text-slate-500">
                Cada técnico aparece como un punto geolocalizado.
              </p>
              <TechniciansMap technicians={technicians} />
            </div>

            <div className="rounded-2xl bg-white p-5 shadow-sm">
              <h3 className="mb-3 text-lg font-semibold">Detalle de incidencia</h3>
              {selectedOrder ? (
                <div className="space-y-3 text-sm">
                  <p>
                    <span className="font-semibold">Incidencia:</span> INC-{selectedOrder.incidence_id}
                  </p>
                  <p>
                    <span className="font-semibold">Cargador:</span> {selectedOrder.charger_id}
                  </p>
                  <p>
                    <span className="font-semibold">Reportada:</span> {new Date(selectedOrder.reported_at).toLocaleString("es-ES")}
                  </p>
                  <p>
                    <span className="font-semibold">Duración estimada:</span> {selectedOrder.estimated_duration_min} min
                  </p>
                  <p>
                    <span className="font-semibold">Duración final:</span> {selectedOrder.final_duration_min ?? "-"} min
                  </p>
                  <p>
                    <span className="font-semibold">Resuelta:</span>{" "}
                    {selectedOrder.resolved_at ? new Date(selectedOrder.resolved_at).toLocaleString("es-ES") : "-"}
                  </p>
                  <p>
                    <span className="font-semibold">Descripción:</span> {selectedOrder.description}
                  </p>

                  <button
                    onClick={() => advanceOrder(selectedOrder.incidence_id)}
                    className="w-full rounded-lg bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700"
                  >
                    Avanzar estado
                  </button>
                </div>
              ) : (
                <p className="text-sm text-slate-500">Selecciona una orden para ver su detalle.</p>
              )}
            </div>

            <div className="rounded-2xl bg-white p-5 shadow-sm">
              <h3 className="mb-3 text-lg font-semibold">Sugerencia de asignación</h3>
              {selectedOrder ? (
                <div className="space-y-2 text-sm">
                  {suggestedTechnicians.length > 0 ? (
                    suggestedTechnicians.map((technician) => {
                      return (
                        <button
                          key={technician.id}
                          onClick={() => assignOrder(selectedOrder.incidence_id, technician.id)}
                          className="flex w-full items-center justify-between rounded-lg border border-slate-200 px-3 py-2 text-left hover:border-sky-300 hover:bg-sky-50"
                        >
                          <span>
                            <span className="font-medium">{technician.name}</span>
                            <span className="block text-xs text-slate-500">{technician.zone}</span>
                          </span>
                          <span className="text-xs font-semibold text-slate-600">
                            Disponible
                          </span>
                        </button>
                      );
                    })
                  ) : (
                    <p className="text-slate-500">No hay técnicos disponibles en este momento.</p>
                  )}
                </div>
              ) : (
                <p className="text-sm text-slate-500">Sin orden seleccionada.</p>
              )}
            </div>

            <div className="rounded-2xl bg-white p-5 shadow-sm">
              <h3 className="mb-3 text-lg font-semibold">Equipo técnico</h3>
              <div className="space-y-2">
                {technicians.map((technician) => (
                  <div key={technician.id} className="rounded-lg border border-slate-200 p-3 text-sm">
                    <p className="font-medium">{technician.name}</p>
                    <p className="text-xs text-slate-500">Último check-in: {technician.lastCheckIn}</p>
                    <div className="mt-2 flex items-center gap-2">
                      <span className="text-xs text-slate-500">Estado:</span>
                      <select
                        value={technician.status}
                        onChange={(event) =>
                          updateTechnicianStatus(
                            technician.id,
                            event.target.value as TechnicianStatus,
                          )
                        }
                        className="rounded border border-slate-200 px-2 py-1 text-xs"
                      >
                        <option value="available">{technicianStatusLabel.available}</option>
                        <option value="on_route">{technicianStatusLabel.on_route}</option>
                        <option value="working">{technicianStatusLabel.working}</option>
                        <option value="offline">{technicianStatusLabel.offline}</option>
                      </select>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </aside>
        </section>
      </main>
    </div>
  );
}

type MetricCardProps = {
  label: string;
  value: number;
};

function MetricCard({ label, value }: MetricCardProps) {
  return (
    <article className="rounded-2xl bg-white p-4 shadow-sm">
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-2 text-2xl font-bold">{value}</p>
    </article>
  );
}

