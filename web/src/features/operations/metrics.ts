import {
  DashboardMetrics,
  OrderFilters,
  Technician,
  WorkOrder,
} from "@/src/features/operations/types";

export function buildDashboardMetrics(
  technicians: Technician[],
  orders: WorkOrder[],
): DashboardMetrics {
  return {
    openOrders: orders.filter((order) => order.status !== "Tancada").length,
    inProgressOrders: orders.filter((order) => order.status === "En curs").length,
    completedOrders: orders.filter((order) => order.status === "Tancada").length,
    availableTechnicians: technicians.filter((technician) => technician.status === "available").length,
  };
}

export function getSuggestedTechnicians(
  _order: WorkOrder,
  technicians: Technician[],
): Technician[] {
  return technicians
    .filter((technician) => technician.status === "available")
    .sort((a, b) => a.name.localeCompare(b.name));
}

export function filterOrders(orders: WorkOrder[], filters: OrderFilters): WorkOrder[] {
  return orders.filter((order) => {
    const chargerPass = filters.charger === "all" || String(order.charger_id) === filters.charger;
    const priorityPass = filters.priority === "all" || order.priority === filters.priority;
    const statusPass = filters.status === "all" || order.status === filters.status;
    const text = `${order.incidence_id} ${order.charger_id} ${order.description}`.toLowerCase();
    const searchPass = !filters.search || text.includes(filters.search.toLowerCase());

    return chargerPass && priorityPass && statusPass && searchPass;
  });
}

export function nextOrderStatus(current: WorkOrder["status"]): WorkOrder["status"] {
  if (current === "Nova") {
    return "Assignada";
  }
  if (current === "Assignada") {
    return "En curs";
  }
  return "Tancada";
}

export function countOrdersInLastHours(
  orders: WorkOrder[],
  hours: number,
  referenceDate = new Date(),
): number {
  const windowMs = hours * 60 * 60 * 1000;
  const referenceMs = referenceDate.getTime();

  return orders.filter((order) => {
    const createdMs = new Date(order.reported_at).getTime();

    if (!Number.isFinite(createdMs)) {
      return false;
    }

    return referenceMs - createdMs <= windowMs && referenceMs - createdMs >= 0;
  }).length;
}

