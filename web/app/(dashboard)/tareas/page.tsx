import { getOperationsSnapshot } from "@/src/features/operations/backend";
import { WorkOrder } from "@/src/features/operations/types";

const statusLabel: Record<WorkOrder["status"], string> = {
  Nova: "Nova",
  Assignada: "Assignada",
  "En curs": "En curs",
  Tancada: "Tancada",
};

const priorityClassName: Record<string, string> = {
  "Reparaci\u00F3 cr\u00EDtica": "text-red-600",
  "Visita de diagnosi": "text-emerald-600",
  "Manteniment preventiu": "text-amber-600",
};

export default async function TareasPage() {
  const snapshot = await getOperationsSnapshot();

  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-8 md:px-6">
      <header className="mb-6 rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-sky-700">Panel de tareas</p>
        <h1 className="mt-2 text-2xl font-bold md:text-3xl">Todas las incidencias del sistema</h1>
        <p className="mt-2 text-sm text-slate-600 md:text-base">
          Vista consolidada de incidencias con el formato recibido por JSON.
        </p>
      </header>

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
            <tr>
              <th className="px-4 py-3">Incidencia</th>
              <th className="px-4 py-3">Cargador</th>
              <th className="px-4 py-3">Reportada</th>
              <th className="px-4 py-3">Prioridad</th>
              <th className="px-4 py-3">Estado</th>
              <th className="px-4 py-3">Descripción</th>
              <th className="px-4 py-3">Duración est.</th>
              <th className="px-4 py-3">Duración fin.</th>
              <th className="px-4 py-3">Resuelta</th>
              <th className="px-4 py-3">Técnico</th>
            </tr>
          </thead>
          <tbody>
            {snapshot.workOrders.map((order) => {
              const technician = snapshot.technicians.find((item) => item.id === order.technicianId);

              return (
                <tr key={order.incidence_id} className="border-t border-slate-200">
                  <td className="px-4 py-3 font-medium">INC-{order.incidence_id}</td>
                  <td className="px-4 py-3">{order.charger_id}</td>
                  <td className="px-4 py-3">{new Date(order.reported_at).toLocaleString("es-ES")}</td>
                  <td className={`px-4 py-3 font-semibold ${priorityClassName[order.priority] ?? "text-slate-700"}`}>
                    {order.priority}
                  </td>
                  <td className="px-4 py-3">{statusLabel[order.status]}</td>
                  <td className="px-4 py-3">{order.description}</td>
                  <td className="px-4 py-3">{order.estimated_duration_min} min</td>
                  <td className="px-4 py-3">{order.final_duration_min ?? "-"} min</td>
                  <td className="px-4 py-3">
                    {order.resolved_at ? new Date(order.resolved_at).toLocaleString("es-ES") : "-"}
                  </td>
                  <td className="px-4 py-3">{technician?.name ?? "Sin asignar"}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </section>
    </main>
  );
}

