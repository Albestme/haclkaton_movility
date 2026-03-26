export type TechnicianStatus = "available" | "on_route" | "working" | "offline";

export type WorkOrderPriority = "high" | "medium" | "low";
export type WorkOrderStatus = "pending" | "assigned" | "in_progress" | "done";

export type GeoPoint = {
  lat: number;
  lng: number;
};

export type Technician = {
  id: string;
  name: string;
  zone: string;
  location: GeoPoint;
  skills: string[];
  status: TechnicianStatus;
  activeOrderId?: string;
  lastCheckIn: string;
};

export type WorkOrder = {
  id: string;
  siteName: string;
  city: string;
  address: string;
  connectorType: string;
  priority: WorkOrderPriority;
  status: WorkOrderStatus;
  scheduledAt: string;
  notes: string;
  technicianId?: string;
};

export type DashboardMetrics = {
  openOrders: number;
  inProgressOrders: number;
  completedOrders: number;
  availableTechnicians: number;
};

export type OrderFilters = {
  city: string;
  priority: "all" | WorkOrderPriority;
  status: "all" | WorkOrderStatus;
  search: string;
};

