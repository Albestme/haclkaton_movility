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

  const chargerOrders = filterOrders(initialWorkOrders, {
    charger: "15",
    priority: "all",
    status: "all",
    search: "",
  });
  assert(chargerOrders.length === 1, "Se esperaba 1 incidencia para cargador 15");

  const selectedOrder = initialWorkOrders[1];
  const suggested = getSuggestedTechnicians(selectedOrder, initialTechnicians);
  assert(suggested.length >= 1, "Debe haber al menos un tecnico sugerido");

  assert(nextOrderStatus("Nova") === "Assignada", "Nova debe pasar a Assignada");
  assert(nextOrderStatus("Assignada") === "En curs", "Assignada debe pasar a En curs");
  assert(nextOrderStatus("En curs") === "Tancada", "En curs debe pasar a Tancada");

  assert(
    initialWorkOrders.every((order) => Number.isFinite(new Date(order.reported_at).getTime())),
    "Cada incidencia debe tener reported_at valido",
  );

  const controlReference = new Date("2024-12-11T08:30:00Z");
  const entriesIn24h = countOrdersInLastHours(initialWorkOrders, 24, controlReference);
  assert(entriesIn24h === 4, "Se esperaban 4 incidencias entrantes en 24 horas");

  console.log("Smoke test OK: logica base operativa valida.");
}

runSmoke();

