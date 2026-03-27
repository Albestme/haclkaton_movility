import { initialTechnicians } from "@/src/features/operations/data";

const technicianStatusLabel: Record<(typeof initialTechnicians)[number]["status"], string> = {
  available: "Disponible",
  on_route: "En ruta",
  working: "Trabajando",
  offline: "Fuera de linea",
};

export default function TecnicosPage() {
  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-8 md:px-6">
      <header className="mb-6 rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-sky-700">Equipo tecnico</p>
        <h1 className="mt-2 text-2xl font-bold md:text-3xl">Tecnicos en el sistema</h1>
      </header>

      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {initialTechnicians.map((technician) => (
          <article key={technician.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-lg font-semibold">{technician.name}</h2>
            <p className="mt-1 text-sm text-slate-500">Zona: {technician.zone}</p>
            <p className="mt-1 text-sm text-slate-500">Estado: {technicianStatusLabel[technician.status]}</p>
            <p className="mt-1 text-sm text-slate-500">Ultimo check-in: {technician.lastCheckIn}</p>
          </article>
        ))}
      </section>
    </main>
  );
}

