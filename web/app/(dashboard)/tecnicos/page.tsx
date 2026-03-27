import TechniciansTaskBoard from "@/src/components/technicians-task-board";
import { getOperationsSnapshot } from "@/src/features/operations/backend";

export default async function TecnicosPage() {
  const snapshot = await getOperationsSnapshot();

  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-8 md:px-6">
      <header className="mb-6 rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-sky-700">Equipo técnico</p>
        <h1 className="mt-2 text-2xl font-bold md:text-3xl">Panel de tareas por técnico</h1>
        <p className="mt-2 text-sm text-slate-600">
          Arrastra cada bloque de tarea para reasignarlo entre técnicos o devolverlo al bloque de no asignadas.
        </p>
      </header>

      <TechniciansTaskBoard technicians={snapshot.technicians} initialOrders={snapshot.workOrders} />
    </main>
  );
}

