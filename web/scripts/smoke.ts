import { initialTechnicians, initialWorkOrders } from "@/src/features/operations/data";
import {
  buildDashboardMetrics,
  countOrdersInLastHours,
  filterOrders,
  getSuggestedTechnicians,
  nextOrderStatus,
} from "@/src/features/operations/metrics";

function assert(condition: boolean, message: string) {
  if (!condition) {
    throw new Error(message);
  }
}

function runSmoke() {
  assert(
    initialTechnicians.every(
      (technician) =>
        Number.isFinite(technician.location.lat) &&
        Number.isFinite(technician.location.lng),
    ),
    "Cada tecnico debe tener coordenadas validas",
  );

  const metrics = buildDashboardMetrics(initialTechnicians, initialWorkOrders);
  assert(metrics.openOrders >= 1, "Debe haber ordenes abiertas en la semilla");

  const barcelonaOrders = filterOrders(initialWorkOrders, {
    city: "Barcelona",
    priority: "all",
    status: "all",
    search: "",
  });
  assert(barcelonaOrders.length === 1, "Se esperaba 1 orden en Barcelona");

  const selectedOrder = initialWorkOrders[1];
  const suggested = getSuggestedTechnicians(selectedOrder, initialTechnicians);
  assert(suggested.length >= 1, "Debe haber al menos un tecnico sugerido");

  assert(nextOrderStatus("pending") === "assigned", "pending debe pasar a assigned");
  assert(nextOrderStatus("assigned") === "in_progress", "assigned debe pasar a in_progress");
  assert(nextOrderStatus("in_progress") === "done", "in_progress debe pasar a done");

  assert(
    initialWorkOrders.every((order) => Number.isFinite(new Date(order.createdAt).getTime())),
    "Cada OT debe tener createdAt valido",
  );

  const controlReference = new Date("2026-03-26T08:30:00Z");
  const entriesIn24h = countOrdersInLastHours(initialWorkOrders, 24, controlReference);
  assert(entriesIn24h === 3, "Se esperaban 3 tareas entrantes en 24 horas");

  console.log("Smoke test OK: logica base operativa valida.");
}

runSmoke();

