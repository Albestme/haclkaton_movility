"use client";

import { useEffect, useMemo, useState } from "react";
import { Fragment } from "react";
import { CircleMarker, MapContainer, Polyline, Popup, TileLayer } from "react-leaflet";
import { GeoPoint, TechnicianStatus } from "@/src/features/operations/types";

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
  offline: "Fuera de linea",
};

async function fetchRoadRoute(start: GeoPoint, end: GeoPoint): Promise<GeoPoint[] | null> {
  const coordinates = `${start.lng},${start.lat};${end.lng},${end.lat}`;
  const response = await fetch(
    `https://router.project-osrm.org/route/v1/driving/${coordinates}?overview=full&geometries=geojson`,
  );

  if (!response.ok) {
    return null;
  }

  const payload = await response.json();
  const geometry = payload?.routes?.[0]?.geometry?.coordinates as [number, number][] | undefined;

  if (!geometry || geometry.length < 2) {
    return null;
  }

  return geometry.map(([lng, lat]) => ({ lat, lng }));
}

export default function TechniciansRoutesMap({ routes }: TechniciansRoutesMapProps) {
  const [resolvedRoutes, setResolvedRoutes] = useState<ResolvedRoute[]>(
    routes.map((route) => ({ ...route, resolvedPath: route.path })),
  );

  useEffect(() => {
    let isCancelled = false;

    async function resolveRoutes() {
      const nextRoutes = await Promise.all(
        routes.map(async (route) => {
          const start = route.path[0];
          const end = route.path[route.path.length - 1];

          if (!start || !end) {
            return { ...route, resolvedPath: route.path };
          }

          try {
            const roadPath = await fetchRoadRoute(start, end);
            return {
              ...route,
              resolvedPath: roadPath ?? route.path,
            };
          } catch {
            return { ...route, resolvedPath: route.path };
          }
        }),
      );

      if (!isCancelled) {
        setResolvedRoutes(nextRoutes);
      }
    }

    setResolvedRoutes(routes.map((route) => ({ ...route, resolvedPath: route.path })));
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
                </div>
              </Popup>
            </CircleMarker>
          </Fragment>
        );
      })}
    </MapContainer>
  );
}

