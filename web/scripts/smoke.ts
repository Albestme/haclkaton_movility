import { initialTechnicians, initialWorkOrders } from "@/src/features/operations/data";
import {
  buildDashboardMetrics,
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

  console.log("Smoke test OK: logica base operativa valida.");
}

runSmoke();

