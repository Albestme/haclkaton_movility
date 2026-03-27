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

const defaultFilters: OrderFilters = {
  city: "all",
  priority: "all",
  status: "all",
  search: "",
};

const technicianStatusLabel: Record<TechnicianStatus, string> = {
  available: "Disponible",
  on_route: "En ruta",
  working: "Trabajando",
  offline: "Fuera de linea",
};

const orderStatusLabel: Record<WorkOrder["status"], string> = {
  pending: "Pendiente",
  assigned: "Asignada",
  in_progress: "En curso",
  done: "Completada",
};

const priorityLabel = {
  correctivo_critico: "Correctivo critico",
  correctivo_no_critico: "Correctivo no critico",
  mantenimiento_preventivo_programado: "Mantenimiento preventivo programado",
  puesta_en_marcha: "Puesta en marcha",
  visita_diagnostico: "Visita de diagnostico",
  high: "Correctivo critico",
  medium: "Mantenimiento preventivo programado",
  low: "Visita de diagnostico",
} as const;

const priorityColor = {
  correctivo_critico: "text-red-600",
  correctivo_no_critico: "text-orange-600",
  mantenimiento_preventivo_programado: "text-amber-600",
  puesta_en_marcha: "text-blue-600",
  visita_diagnostico: "text-emerald-600",
  high: "text-red-600",
  medium: "text-amber-600",
  low: "text-emerald-600",
} as const;

const TechniciansMap = dynamic(() => import("@/src/components/technicians-map"), {
  ssr: false,
  loading: () => (
    <div className="flex h-[360px] items-center justify-center rounded-xl border border-slate-200 bg-slate-50 text-sm text-slate-500">
      Cargando mapa...
    </div>
  ),
});

export default function OperationsDashboard() {
  const [technicians, setTechnicians] = useState<Technician[]>(initialTechnicians);
  const [orders, setOrders] = useState<WorkOrder[]>(initialWorkOrders);
  const [filters, setFilters] = useState<OrderFilters>(defaultFilters);
  const [selectedOrderId, setSelectedOrderId] = useState<string>(initialWorkOrders[0]?.id ?? "");

  const filteredOrders = useMemo(() => filterOrders(orders, filters), [orders, filters]);
  const selectedOrder = useMemo(
    () => orders.find((order) => order.id === selectedOrderId) ?? null,
    [orders, selectedOrderId],
  );

  const suggestedTechnicians = useMemo(() => {
    if (!selectedOrder) {
      return [];
    }
    return getSuggestedTechnicians(selectedOrder, technicians);
  }, [selectedOrder, technicians]);

  const metrics = useMemo(() => buildDashboardMetrics(technicians, orders), [technicians, orders]);
  const cities = useMemo(() => ["all", ...new Set(orders.map((order) => order.city))], [orders]);

  function assignOrder(orderId: string, technicianId: string) {
    setOrders((current) =>
      current.map((order) =>
        order.id === orderId
          ? {
              ...order,
              technicianId,
              status: order.status === "pending" ? "assigned" : order.status,
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

  function advanceOrder(orderId: string) {
    setOrders((current) =>
      current.map((order) =>
        order.id === orderId
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
          <h1 className="text-2xl font-bold md:text-3xl">Centro de gestion de tecnicos</h1>
          <p className="text-sm text-slate-600 md:text-base">
            Base inicial para asignar incidencias, seguir estados y priorizar mantenimiento de cargadores.
          </p>
        </header>

        <section className="grid gap-4 md:grid-cols-4">
          <MetricCard label="OT abiertas" value={metrics.openOrders} />
          <MetricCard label="OT en curso" value={metrics.inProgressOrders} />
          <MetricCard label="OT completadas" value={metrics.completedOrders} />
          <MetricCard label="Tecnicos disponibles" value={metrics.availableTechnicians} />
        </section>

        <section className="grid gap-6 lg:grid-cols-[1.3fr_0.9fr]">
          <div className="space-y-4 rounded-2xl bg-white p-5 shadow-sm">
            <h2 className="text-lg font-semibold">Ordenes de trabajo</h2>

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
                placeholder="Buscar OT, ubicacion o nota"
              />

              <select
                value={filters.city}
                onChange={(event) =>
                  setFilters((current) => ({
                    ...current,
                    city: event.target.value,
                  }))
                }
                className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
              >
                {cities.map((city) => (
                  <option key={city} value={city}>
                    {city === "all" ? "Todas las ciudades" : city}
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
                <option value="correctivo_critico">Correctivo critico</option>
                <option value="correctivo_no_critico">Correctivo no critico</option>
                <option value="mantenimiento_preventivo_programado">Mantenimiento preventivo programado</option>
                <option value="puesta_en_marcha">Puesta en marcha</option>
                <option value="visita_diagnostico">Visita de diagnostico</option>
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
                <option value="pending">Pendiente</option>
                <option value="assigned">Asignada</option>
                <option value="in_progress">En curso</option>
                <option value="done">Completada</option>
              </select>
            </div>

            <div className="overflow-hidden rounded-xl border border-slate-200">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="px-3 py-3">OT</th>
                    <th className="px-3 py-3">Sitio</th>
                    <th className="px-3 py-3">Prioridad</th>
                    <th className="px-3 py-3">Estado</th>
                    <th className="px-3 py-3">Tecnico</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredOrders.map((order) => {
                    const assignedTech = technicians.find((technician) => technician.id === order.technicianId);
                    const isSelected = selectedOrderId === order.id;

                    return (
                      <tr
                        key={order.id}
                        onClick={() => setSelectedOrderId(order.id)}
                        className={`cursor-pointer border-t border-slate-200 ${
                          isSelected ? "bg-sky-50" : "hover:bg-slate-50"
                        }`}
                      >
                        <td className="px-3 py-3 font-medium">{order.id}</td>
                        <td className="px-3 py-3">
                          <p>{order.siteName}</p>
                          <p className="text-xs text-slate-500">{order.city}</p>
                        </td>
                        <td className={`px-3 py-3 font-semibold ${priorityColor[order.priority]}`}>
                          {priorityLabel[order.priority]}
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
              <h3 className="mb-3 text-lg font-semibold">Mapa de tecnicos (OpenStreetMap)</h3>
              <p className="mb-3 text-sm text-slate-500">
                Cada tecnico aparece como un punto geolocalizado.
              </p>
              <TechniciansMap technicians={technicians} />
            </div>

            <div className="rounded-2xl bg-white p-5 shadow-sm">
              <h3 className="mb-3 text-lg font-semibold">Detalle de orden</h3>
              {selectedOrder ? (
                <div className="space-y-3 text-sm">
                  <p>
                    <span className="font-semibold">Sitio:</span> {selectedOrder.siteName}
                  </p>
                  <p>
                    <span className="font-semibold">Direccion:</span> {selectedOrder.address}
                  </p>
                  <p>
                    <span className="font-semibold">Conector:</span> {selectedOrder.connectorType}
                  </p>
                  <p>
                    <span className="font-semibold">Hora:</span> {selectedOrder.scheduledAt}
                  </p>
                  <p>
                    <span className="font-semibold">Nota:</span> {selectedOrder.notes}
                  </p>

                  <button
                    onClick={() => advanceOrder(selectedOrder.id)}
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
              <h3 className="mb-3 text-lg font-semibold">Sugerencia de asignacion</h3>
              {selectedOrder ? (
                <div className="space-y-2 text-sm">
                  {suggestedTechnicians.length > 0 ? (
                    suggestedTechnicians.map((technician) => {
                      const isSkillMatch = technician.skills.includes(selectedOrder.connectorType);

                      return (
                        <button
                          key={technician.id}
                          onClick={() => assignOrder(selectedOrder.id, technician.id)}
                          className="flex w-full items-center justify-between rounded-lg border border-slate-200 px-3 py-2 text-left hover:border-sky-300 hover:bg-sky-50"
                        >
                          <span>
                            <span className="font-medium">{technician.name}</span>
                            <span className="block text-xs text-slate-500">{technician.zone}</span>
                          </span>
                          <span className="text-xs font-semibold text-slate-600">
                            {isSkillMatch ? "Match skill" : "Generalista"}
                          </span>
                        </button>
                      );
                    })
                  ) : (
                    <p className="text-slate-500">No hay tecnicos disponibles en este momento.</p>
                  )}
                </div>
              ) : (
                <p className="text-sm text-slate-500">Sin orden seleccionada.</p>
              )}
            </div>

            <div className="rounded-2xl bg-white p-5 shadow-sm">
              <h3 className="mb-3 text-lg font-semibold">Equipo tecnico</h3>
              <div className="space-y-2">
                {technicians.map((technician) => (
                  <div key={technician.id} className="rounded-lg border border-slate-200 p-3 text-sm">
                    <p className="font-medium">{technician.name}</p>
                    <p className="text-xs text-slate-500">Ultimo check-in: {technician.lastCheckIn}</p>
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

