import {
  initialTechnicians,
  initialWorkOrders,
} from "@/src/features/operations/data";
import { WorkOrder } from "@/src/features/operations/types";

const statusLabel: Record<WorkOrder["status"], string> = {
  pending: "Pendiente",
  assigned: "Asignada",
  in_progress: "En curso",
  done: "Completada",
};

const priorityLabel: Record<WorkOrder["priority"], string> = {
  correctivo_critico: "Correctivo critico",
  correctivo_no_critico: "Correctivo no critico",
  mantenimiento_preventivo_programado: "Mantenimiento preventivo programado",
  puesta_en_marcha: "Puesta en marcha",
  visita_diagnostico: "Visita de diagnostico",
  high: "Correctivo critico",
  medium: "Mantenimiento preventivo programado",
  low: "Visita de diagnostico",
};

const priorityClassName: Record<WorkOrder["priority"], string> = {
  correctivo_critico: "text-red-600",
  correctivo_no_critico: "text-orange-600",
  mantenimiento_preventivo_programado: "text-amber-600",
  puesta_en_marcha: "text-blue-600",
  visita_diagnostico: "text-emerald-600",
  high: "text-red-600",
  medium: "text-amber-600",
  low: "text-emerald-600",
};

export default function TareasPage() {
  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-8 md:px-6">
      <header className="mb-6 rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-sky-700">Panel de tareas</p>
        <h1 className="mt-2 text-2xl font-bold md:text-3xl">Todas las tareas del sistema</h1>
        <p className="mt-2 text-sm text-slate-600 md:text-base">
          Vista consolidada de ordenes de trabajo activas para planificar prioridad, estado y asignacion.
        </p>
      </header>

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
            <tr>
              <th className="px-4 py-3">OT</th>
              <th className="px-4 py-3">Sitio</th>
              <th className="px-4 py-3">Ciudad</th>
              <th className="px-4 py-3">Conector</th>
              <th className="px-4 py-3">Prioridad</th>
              <th className="px-4 py-3">Estado</th>
              <th className="px-4 py-3">Tecnico</th>
              <th className="px-4 py-3">Horario</th>
            </tr>
          </thead>
          <tbody>
            {initialWorkOrders.map((order) => {
              const technician = initialTechnicians.find((item) => item.id === order.technicianId);

              return (
                <tr key={order.id} className="border-t border-slate-200">
                  <td className="px-4 py-3 font-medium">{order.id}</td>
                  <td className="px-4 py-3">{order.siteName}</td>
                  <td className="px-4 py-3">{order.city}</td>
                  <td className="px-4 py-3">{order.connectorType}</td>
                  <td className={`px-4 py-3 font-semibold ${priorityClassName[order.priority]}`}>
                    {priorityLabel[order.priority]}
                  </td>
                  <td className="px-4 py-3">{statusLabel[order.status]}</td>
                  <td className="px-4 py-3">{technician?.name ?? "Sin asignar"}</td>
                  <td className="px-4 py-3">{order.scheduledAt}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </section>
    </main>
  );
}

