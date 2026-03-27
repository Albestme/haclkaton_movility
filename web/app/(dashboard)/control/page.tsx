"use client";

import { useMemo, useState } from "react";
import { initialWorkOrders } from "@/src/features/operations/data";
import { countOrdersInLastHours } from "@/src/features/operations/metrics";
import { WorkOrder } from "@/src/features/operations/types";

const intervalOptions = [
  { hours: 6, label: "Últimas 6 horas" },
  { hours: 12, label: "Últimas 12 horas" },
  { hours: 24, label: "Últimas 24 horas" },
  { hours: 48, label: "Últimas 48 horas" },
];

const statusOptions = [
  { value: "all", label: "Todas" },
  { value: "active", label: "Activas" },
  { value: "done", label: "Finalizadas" },
] as const;

type StatusFilter = (typeof statusOptions)[number]["value"];

const statusLabel: Record<WorkOrder["status"], string> = {
  pending: "Pendiente",
  assigned: "Asignada",
  in_progress: "En curso",
  done: "Completada",
};

const priorityLabel: Record<WorkOrder["priority"], string> = {
  correctivo_critico: "Correctivo crítico",
  correctivo_no_critico: "Correctivo no crítico",
  mantenimiento_preventivo_programado: "Mantenimiento preventivo programado",
  puesta_en_marcha: "Puesta en marcha",
  visita_diagnostico: "Visita de diagnóstico",
  high: "Correctivo crítico",
  medium: "Mantenimiento preventivo programado",
  low: "Visita de diagnóstico",
};

function getReferenceDate() {
  const latestTimestamp = Math.max(
    ...initialWorkOrders.map((order) => new Date(order.createdAt).getTime()),
  );

  return new Date(latestTimestamp);
}

export default function ControlPage() {
  const [hours, setHours] = useState<number>(24);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all");

  const referenceDate = useMemo(() => getReferenceDate(), []);
  const ordersInWindow = useMemo(() => {
    const windowMs = hours * 60 * 60 * 1000;
    const referenceMs = referenceDate.getTime();

    return initialWorkOrders.filter((order) => {
      const createdMs = new Date(order.createdAt).getTime();
      return Number.isFinite(createdMs) && referenceMs - createdMs <= windowMs && referenceMs - createdMs >= 0;
    });
  }, [hours, referenceDate]);

  const ordersCount = useMemo(
    () => countOrdersInLastHours(initialWorkOrders, hours, referenceDate),
    [hours, referenceDate],
  );

  const filteredOrders = useMemo(() => {
    if (statusFilter === "all") {
      return ordersInWindow;
    }

    return ordersInWindow.filter((order) =>
      statusFilter === "done" ? order.status === "done" : order.status !== "done",
    );
  }, [ordersInWindow, statusFilter]);

  const percentage = Math.round((ordersCount / initialWorkOrders.length) * 100);

  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-8 md:px-6">
      <header className="mb-6 rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-sky-700">Panel de control</p>
        <h1 className="mt-2 text-2xl font-bold md:text-3xl">Entradas de tareas por tiempo</h1>
        <p className="mt-2 text-sm text-slate-600 md:text-base">
          Selecciona un intervalo para ver cuántas tareas entraron en ese período.
        </p>
      </header>

      <section className="grid gap-4 md:grid-cols-[1fr_1fr]">
        <article className="rounded-2xl bg-white p-6 shadow-sm">
          <label htmlFor="time-window" className="mb-2 block text-sm font-medium text-slate-700">
            Ventana de tiempo
          </label>
          <select
            id="time-window"
            value={hours}
            onChange={(event) => setHours(Number(event.target.value))}
            className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm"
          >
            {intervalOptions.map((option) => (
              <option key={option.hours} value={option.hours}>
                {option.label}
              </option>
            ))}
          </select>

          <p className="mt-4 text-xs text-slate-500">
            Referencia de cálculo: {referenceDate.toLocaleString("es-ES")}
          </p>

          <label htmlFor="status-filter" className="mb-2 mt-4 block text-sm font-medium text-slate-700">
            Estado de tareas
          </label>
          <select
            id="status-filter"
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value as StatusFilter)}
            className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm"
          >
            {statusOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </article>

        <article className="rounded-2xl bg-white p-6 shadow-sm">
          <p className="text-sm text-slate-500">Tareas que han entrado</p>
          <p className="mt-2 text-4xl font-bold text-slate-900">{ordersCount}</p>
          <p className="mt-2 text-sm text-slate-600">
            {percentage}% del total de tareas ({initialWorkOrders.length}) en la ventana seleccionada.
          </p>

          <div className="mt-4 h-3 w-full overflow-hidden rounded-full bg-slate-200">
            <div className="h-full rounded-full bg-sky-600" style={{ width: `${percentage}%` }} />
          </div>
        </article>
      </section>

      <section className="mt-6 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <header className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
          <h2 className="text-sm font-semibold text-slate-800">Tareas en el intervalo seleccionado</h2>
          <span className="text-xs text-slate-500">Mostrando {filteredOrders.length}</span>
        </header>

        {filteredOrders.length === 0 ? (
          <p className="px-4 py-6 text-sm text-slate-500">No hay tareas para ese filtro de tiempo/estado.</p>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
              <tr>
                <th className="px-4 py-3">OT</th>
                <th className="px-4 py-3">Sitio</th>
                <th className="px-4 py-3">Entrada</th>
                <th className="px-4 py-3">Prioridad</th>
                <th className="px-4 py-3">Estado</th>
              </tr>
            </thead>
            <tbody>
              {filteredOrders.map((order) => (
                <tr key={order.id} className="border-t border-slate-200">
                  <td className="px-4 py-3 font-medium">{order.id}</td>
                  <td className="px-4 py-3">{order.siteName}</td>
                  <td className="px-4 py-3">{new Date(order.createdAt).toLocaleString("es-ES")}</td>
                  <td className="px-4 py-3">{priorityLabel[order.priority]}</td>
                  <td className="px-4 py-3">{statusLabel[order.status]}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </main>
  );
}

