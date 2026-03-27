import { initialTechnicians, initialWorkOrders } from "@/src/features/operations/data";
import {
  GeoPoint,
  Technician,
  TechnicianStatus,
  WorkOrder,
  WorkOrderStatus,
} from "@/src/features/operations/types";

type BackendTechnician = {
  technician_id: number;
  name: string;
  zone: string;
  latitude: number;
  longitude: number;
};

type BackendIncidence = {
  incidence_id: number;
  charger_id: number;
  reported_at: string;
  priority?: string | null;
  status?: string | null;
  description?: string | null;
  estimated_duration_min?: number | null;
  final_duration_min?: number | null;
  resolved_at?: string | null;
};

type BackendRouteStop = {
  visit_id: number;
  incidence_id?: number | null;
  priority?: string | null;
  status?: string | null;
  planned_date?: string | null;
  estimated_duration_min?: number | null;
  latitude?: number | null;
  longitude?: number | null;
};

export type BackendTechnicianRoute = {
  technicianId: string;
  technicianName: string;
  status: TechnicianStatus;
  orderId: string;
  orderSiteName: string;
  path: GeoPoint[];
};

export type OperationsSnapshot = {
  technicians: Technician[];
  workOrders: WorkOrder[];
  routes: BackendTechnicianRoute[];
};

const DEFAULT_BACKEND_URL = "http://127.0.0.1:8000";

function backendBaseUrl() {
  return (process.env.BACKEND_API_URL ?? DEFAULT_BACKEND_URL).replace(/\/$/, "");
}

async function fetchBackend<T>(path: string): Promise<T> {
  const response = await fetch(`${backendBaseUrl()}${path}`, {
    cache: "no-store",
    signal: AbortSignal.timeout(7000),
  });

  if (!response.ok) {
    throw new Error(`Backend request failed: ${response.status} ${response.statusText}`);
  }

  return (await response.json()) as T;
}

function normalizeStatus(status: string | null | undefined): WorkOrderStatus {
  if (status === "Nova" || status === "Assignada" || status === "En curs" || status === "Tancada") {
    return status;
  }
  if (status === "Pendent") {
    return "Nova";
  }
  return "Nova";
}

function toTechnicianId(backendId: number): string {
  return `tec-${String(backendId).padStart(2, "0")}`;
}

function inferTechnicianStatus(stops: BackendRouteStop[]): TechnicianStatus {
  if (stops.some((stop) => stop.status === "in_progress")) {
    return "working";
  }
  if (stops.length > 0) {
    return "on_route";
  }
  return "available";
}

function toWorkOrder(item: BackendIncidence): WorkOrder {
  return {
    incidence_id: item.incidence_id,
    charger_id: item.charger_id,
    reported_at: item.reported_at,
    priority: item.priority ?? "Sin prioridad",
    status: normalizeStatus(item.status),
    description: item.description ?? "Sin descripción",
    estimated_duration_min: item.estimated_duration_min ?? 0,
    final_duration_min: item.final_duration_min ?? undefined,
    resolved_at: item.resolved_at ?? undefined,
  };
}

function safePoint(latitude?: number | null, longitude?: number | null): GeoPoint | null {
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
    return null;
  }

  return { lat: Number(latitude), lng: Number(longitude) };
}

export async function getOperationsSnapshot(): Promise<OperationsSnapshot> {
  try {
    const backendTechnicians = await fetchBackend<BackendTechnician[]>("/api/v1/app/technicians");

    const backendIncidences = await fetchBackend<BackendIncidence[]>("/api/v1/app/incidences").catch(
      () => [] as BackendIncidence[],
    );

    const routeResults = await Promise.all(
      backendTechnicians.map(async (technician) => {
        try {
          const route = await fetchBackend<BackendRouteStop[]>(
            `/api/v1/app/technicians/${technician.technician_id}/route`,
          );
          return { technicianId: technician.technician_id, route };
        } catch {
          return { technicianId: technician.technician_id, route: [] as BackendRouteStop[] };
        }
      }),
    );

    const routeByTechnician = new Map<number, BackendRouteStop[]>(
      routeResults.map((result) => [result.technicianId, result.route]),
    );

    const assignmentByIncidence = new Map<number, string>();
    routeByTechnician.forEach((stops, backendTechnicianId) => {
      const technicianId = toTechnicianId(backendTechnicianId);
      stops.forEach((stop) => {
        if (Number.isFinite(stop.incidence_id)) {
          assignmentByIncidence.set(Number(stop.incidence_id), technicianId);
        }
      });
    });

    const technicians = backendTechnicians.map((item) => {
      const stops = routeByTechnician.get(item.technician_id) ?? [];
      const activeStop = stops.find((stop) => Number.isFinite(stop.incidence_id));

      return {
        id: toTechnicianId(item.technician_id),
        backendId: item.technician_id,
        name: item.name,
        zone: item.zone,
        location: { lat: item.latitude, lng: item.longitude },
        skills: [],
        status: inferTechnicianStatus(stops),
        activeOrderId: Number.isFinite(activeStop?.incidence_id)
          ? Number(activeStop?.incidence_id)
          : undefined,
        lastCheckIn: "--:--",
      } as Technician;
    });

    const workOrders = backendIncidences.map((item) => {
      const mapped = toWorkOrder(item);
      return {
        ...mapped,
        technicianId: assignmentByIncidence.get(mapped.incidence_id),
      };
    });

    const routes: BackendTechnicianRoute[] = technicians
      .map((technician) => {
        const backendId = technician.backendId;
        if (!backendId) {
          return null;
        }

        const stops = routeByTechnician.get(backendId) ?? [];
        const routeStop = stops.find((stop) => safePoint(stop.latitude, stop.longitude));
        const destination = safePoint(routeStop?.latitude, routeStop?.longitude);

        if (!destination) {
          return null;
        }

        const orderId = Number.isFinite(routeStop?.incidence_id)
          ? `INC-${routeStop?.incidence_id}`
          : `VISIT-${routeStop?.visit_id ?? "N/A"}`;

        return {
          technicianId: technician.id,
          technicianName: technician.name,
          status: technician.status,
          orderId,
          orderSiteName: `Destino ${technician.zone}`,
          path: [technician.location, destination],
        };
      })
      .filter((route): route is BackendTechnicianRoute => route !== null);

    return { technicians, workOrders, routes };
  } catch {
    return {
      technicians: initialTechnicians,
      workOrders: initialWorkOrders,
      routes: [],
    };
  }
}

