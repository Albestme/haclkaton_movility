"use client";

import { useEffect, useMemo, useState } from "react";
import { Fragment } from "react";
import { CircleMarker, MapContainer, Polyline, Popup, TileLayer } from "react-leaflet";
import { GeoPoint, TechnicianStatus } from "@/src/features/operations/types";
import {
  formatDistance,
  formatDuration,
  resolveRoadRoute,
} from "@/src/features/operations/routing";

export type TechnicianRoute = {
  technicianId: string;
  technicianName: string;
  status: TechnicianStatus;
  orderId: string;
  orderSiteName: string;
  path: GeoPoint[];
};

type TechniciansRoutesMapProps = {
  routes: TechnicianRoute[];
};

type ResolvedRoute = TechnicianRoute & {
  resolvedPath: GeoPoint[];
  distanceMeters: number;
  durationSeconds: number;
  source: "osrm" | "fallback";
};

const routeColorByStatus: Record<TechnicianStatus, string> = {
  available: "#16a34a",
  on_route: "#2563eb",
  working: "#ea580c",
  offline: "#64748b",
};

const statusLabel: Record<TechnicianStatus, string> = {
  available: "Disponible",
  on_route: "En ruta",
  working: "Trabajando",
  offline: "Fuera de línea",
};

export default function TechniciansRoutesMap({ routes }: TechniciansRoutesMapProps) {
  const [resolvedRoutes, setResolvedRoutes] = useState<ResolvedRoute[]>(
    routes.map((route) => ({
      ...route,
      resolvedPath: route.path,
      distanceMeters: 0,
      durationSeconds: 0,
      source: "fallback",
    })),
  );

  useEffect(() => {
    let isCancelled = false;

    async function resolveRoutes() {
      const nextRoutes = await Promise.all(
        routes.map(async (route) => {
          const start = route.path[0];
          const end = route.path[route.path.length - 1];

          if (!start || !end) {
            return {
              ...route,
              resolvedPath: route.path,
              distanceMeters: 0,
              durationSeconds: 0,
              source: "fallback" as const,
            };
          }

          try {
            const roadRoute = await resolveRoadRoute(start, end, route.path);
            return {
              ...route,
              resolvedPath: roadRoute.path,
              distanceMeters: roadRoute.distanceMeters,
              durationSeconds: roadRoute.durationSeconds,
              source: roadRoute.source,
            };
          } catch {
            return {
              ...route,
              resolvedPath: route.path,
              distanceMeters: 0,
              durationSeconds: 0,
              source: "fallback" as const,
            };
          }
        }),
      );

      if (!isCancelled) {
        setResolvedRoutes(nextRoutes);
      }
    }

    setResolvedRoutes(
      routes.map((route) => ({
        ...route,
        resolvedPath: route.path,
        distanceMeters: 0,
        durationSeconds: 0,
        source: "fallback" as const,
      })),
    );
    void resolveRoutes();

    return () => {
      isCancelled = true;
    };
  }, [routes]);

  const center = useMemo(() => {
    const points = resolvedRoutes.flatMap((route) => route.resolvedPath);

    if (points.length === 0) {
      return { lat: 41.3874, lng: 2.1686 };
    }

    const sum = points.reduce(
      (acc, point) => ({ lat: acc.lat + point.lat, lng: acc.lng + point.lng }),
      { lat: 0, lng: 0 },
    );

    return {
      lat: sum.lat / points.length,
      lng: sum.lng / points.length,
    };
  }, [resolvedRoutes]);

  return (
    <MapContainer center={[center.lat, center.lng]} zoom={8} className="technicians-map" scrollWheelZoom>
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      {resolvedRoutes.map((route) => {
        const color = routeColorByStatus[route.status];
        const first = route.resolvedPath[0];
        const last = route.resolvedPath[route.resolvedPath.length - 1];

        if (!first || !last) {
          return null;
        }

        return (
          <Fragment key={`${route.technicianId}-${route.orderId}`}>
            <Polyline
              positions={route.resolvedPath.map((point) => [point.lat, point.lng])}
              pathOptions={{ color, weight: 4, opacity: 0.85 }}
            />

            <CircleMarker
              center={[first.lat, first.lng]}
              radius={7}
              pathOptions={{ color: "#ffffff", fillColor: color, fillOpacity: 1, weight: 2 }}
            >
              <Popup>
                <div className="space-y-1 text-sm">
                  <p className="font-semibold">{route.technicianName}</p>
                  <p>Estado: {statusLabel[route.status]}</p>
                  <p>Salida de ruta</p>
                  <p>Distancia: {formatDistance(route.distanceMeters)}</p>
                  <p>ETA: {formatDuration(route.durationSeconds)}</p>
                </div>
              </Popup>
            </CircleMarker>

            <CircleMarker
              center={[last.lat, last.lng]}
              radius={7}
              pathOptions={{ color: "#ffffff", fillColor: "#0f172a", fillOpacity: 1, weight: 2 }}
            >
              <Popup>
                <div className="space-y-1 text-sm">
                  <p className="font-semibold">{route.orderSiteName}</p>
                  <p>OT: {route.orderId}</p>
                  <p>Destino de ruta</p>
                  <p>Distancia: {formatDistance(route.distanceMeters)}</p>
                  <p>ETA: {formatDuration(route.durationSeconds)}</p>
                  <p className="text-xs text-slate-500">
                    {route.source === "osrm" ? "Ruta vial (OSRM)" : "Estimación local"}
                  </p>
                </div>
              </Popup>
            </CircleMarker>
          </Fragment>
        );
      })}
    </MapContainer>
  );
}

