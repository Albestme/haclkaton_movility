export type TechnicianStatus = "available" | "on_route" | "working" | "offline";

export type WorkOrderPriority = string;
export type WorkOrderStatus = "Nova" | "Assignada" | "En curs" | "Tancada";

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
  activeOrderId?: number;
  lastCheckIn: string;
};

export type WorkOrder = {
  incidence_id: number;
  charger_id: number;
  reported_at: string;
  priority: WorkOrderPriority;
  status: WorkOrderStatus;
  description: string;
  estimated_duration_min: number;
  final_duration_min?: number;
  resolved_at?: string;
  technicianId?: string;
};

export type DashboardMetrics = {
  openOrders: number;
  inProgressOrders: number;
  completedOrders: number;
  availableTechnicians: number;
};

export type OrderFilters = {
  charger: string;
  priority: "all" | WorkOrderPriority;
  status: "all" | WorkOrderStatus;
  search: string;
};

