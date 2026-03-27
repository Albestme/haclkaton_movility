"use client";

import { useMemo } from "react";
import { CircleMarker, MapContainer, Popup, TileLayer } from "react-leaflet";
import { Technician, TechnicianStatus } from "@/src/features/operations/types";

type TechniciansMapProps = {
  technicians: Technician[];
};

const statusColor: Record<TechnicianStatus, string> = {
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

export default function TechniciansMap({ technicians }: TechniciansMapProps) {
  const center = useMemo(() => {
    if (technicians.length === 0) {
      return { lat: 41.3874, lng: 2.1686 };
    }

    const sum = technicians.reduce(
      (acc, technician) => ({
        lat: acc.lat + technician.location.lat,
        lng: acc.lng + technician.location.lng,
      }),
      { lat: 0, lng: 0 },
    );

    return {
      lat: sum.lat / technicians.length,
      lng: sum.lng / technicians.length,
    };
  }, [technicians]);

  return (
    <MapContainer
      center={[center.lat, center.lng]}
      zoom={8}
      className="technicians-map"
      scrollWheelZoom
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      {technicians.map((technician) => (
        <CircleMarker
          key={technician.id}
          center={[technician.location.lat, technician.location.lng]}
          radius={9}
          pathOptions={{
            color: "#ffffff",
            fillColor: statusColor[technician.status],
            fillOpacity: 0.9,
            weight: 2,
          }}
        >
          <Popup>
            <div className="space-y-1 text-sm">
              <p className="font-semibold">{technician.name}</p>
              <p>Zona: {technician.zone}</p>
              <p>Estado: {statusLabel[technician.status]}</p>
              <p>Último check-in: {technician.lastCheckIn}</p>
            </div>
          </Popup>
        </CircleMarker>
      ))}
    </MapContainer>
  );
}

