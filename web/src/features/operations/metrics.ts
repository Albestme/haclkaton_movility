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
    openOrders: orders.filter((order) => order.status === "pending" || order.status === "assigned")
      .length,
    inProgressOrders: orders.filter((order) => order.status === "in_progress").length,
    completedOrders: orders.filter((order) => order.status === "done").length,
    availableTechnicians: technicians.filter((technician) => technician.status === "available").length,
  };
}

export function getSuggestedTechnicians(
  order: WorkOrder,
  technicians: Technician[],
): Technician[] {
  return technicians
    .filter((technician) => technician.status === "available")
    .sort((a, b) => {
      const aHasSkill = a.skills.includes(order.connectorType) ? 1 : 0;
      const bHasSkill = b.skills.includes(order.connectorType) ? 1 : 0;

      if (aHasSkill !== bHasSkill) {
        return bHasSkill - aHasSkill;
      }

      return a.name.localeCompare(b.name);
    });
}

export function filterOrders(orders: WorkOrder[], filters: OrderFilters): WorkOrder[] {
  return orders.filter((order) => {
    const cityPass = filters.city === "all" || order.city === filters.city;
    const priorityPass = filters.priority === "all" || order.priority === filters.priority;
    const statusPass = filters.status === "all" || order.status === filters.status;
    const text = `${order.id} ${order.siteName} ${order.address} ${order.notes}`.toLowerCase();
    const searchPass = !filters.search || text.includes(filters.search.toLowerCase());

    return cityPass && priorityPass && statusPass && searchPass;
  });
}

export function nextOrderStatus(current: WorkOrder["status"]): WorkOrder["status"] {
  if (current === "pending") {
    return "assigned";
  }
  if (current === "assigned") {
    return "in_progress";
  }
  return "done";
}

export function countOrdersInLastHours(
  orders: WorkOrder[],
  hours: number,
  referenceDate = new Date(),
): number {
  const windowMs = hours * 60 * 60 * 1000;
  const referenceMs = referenceDate.getTime();

  return orders.filter((order) => {
    const createdMs = new Date(order.createdAt).getTime();

    if (!Number.isFinite(createdMs)) {
      return false;
    }

    return referenceMs - createdMs <= windowMs && referenceMs - createdMs >= 0;
  }).length;
}

