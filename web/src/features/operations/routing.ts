import { GeoPoint } from "@/src/features/operations/types";

const DEFAULT_OSRM_BASE_URL = "https://router.project-osrm.org";
const AVERAGE_SPEED_KMH = 35;

export type RoutingResult = {
  path: GeoPoint[];
  distanceMeters: number;
  durationSeconds: number;
  source: "osrm" | "fallback";
};

const routeCache = new Map<string, RoutingResult>();

function normalizeBaseUrl(value: string): string {
  return value.endsWith("/") ? value.slice(0, -1) : value;
}

function buildCacheKey(baseUrl: string, start: GeoPoint, end: GeoPoint): string {
  return `${baseUrl}:${start.lat.toFixed(5)},${start.lng.toFixed(5)}->${end.lat.toFixed(5)},${end.lng.toFixed(5)}`;
}

function calculatePathDistanceMeters(path: GeoPoint[]): number {
  if (path.length < 2) {
    return 0;
  }

  let total = 0;

  for (let index = 1; index < path.length; index += 1) {
    const prev = path[index - 1];
    const next = path[index];
    total += haversineMeters(prev, next);
  }

  return total;
}

function haversineMeters(a: GeoPoint, b: GeoPoint): number {
  const earthRadius = 6371000;
  const toRad = (value: number) => (value * Math.PI) / 180;

  const deltaLat = toRad(b.lat - a.lat);
  const deltaLng = toRad(b.lng - a.lng);
  const sinLat = Math.sin(deltaLat / 2);
  const sinLng = Math.sin(deltaLng / 2);
  const q = sinLat * sinLat + Math.cos(toRad(a.lat)) * Math.cos(toRad(b.lat)) * sinLng * sinLng;

  return 2 * earthRadius * Math.atan2(Math.sqrt(q), Math.sqrt(1 - q));
}

function estimateDurationSeconds(distanceMeters: number): number {
  const speedMetersPerSecond = (AVERAGE_SPEED_KMH * 1000) / 3600;
  if (!Number.isFinite(distanceMeters) || distanceMeters <= 0) {
    return 0;
  }
  return Math.round(distanceMeters / speedMetersPerSecond);
}

export function formatDistance(distanceMeters: number): string {
  return `${(distanceMeters / 1000).toFixed(1)} km`;
}

export function formatDuration(durationSeconds: number): string {
  const totalMinutes = Math.max(1, Math.round(durationSeconds / 60));
  if (totalMinutes < 60) {
    return `${totalMinutes} min`;
  }

  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  return minutes === 0 ? `${hours} h` : `${hours} h ${minutes} min`;
}

export function getRoutingBaseUrl(): string {
  const configured = process.env.NEXT_PUBLIC_OSRM_BASE_URL;
  if (!configured) {
    return DEFAULT_OSRM_BASE_URL;
  }
  return normalizeBaseUrl(configured);
}

function toFallbackResult(fallbackPath: GeoPoint[]): RoutingResult {
  const distanceMeters = calculatePathDistanceMeters(fallbackPath);

  return {
    path: fallbackPath,
    distanceMeters,
    durationSeconds: estimateDurationSeconds(distanceMeters),
    source: "fallback",
  };
}

export async function resolveRoadRoute(
  start: GeoPoint,
  end: GeoPoint,
  fallbackPath: GeoPoint[],
): Promise<RoutingResult> {
  const baseUrl = getRoutingBaseUrl();
  const cacheKey = buildCacheKey(baseUrl, start, end);
  const cached = routeCache.get(cacheKey);

  if (cached) {
    return cached;
  }

  const safeFallback = fallbackPath.length >= 2 ? fallbackPath : [start, end];
  const coordinates = `${start.lng},${start.lat};${end.lng},${end.lat}`;

  try {
    const response = await fetch(
      `${baseUrl}/route/v1/driving/${coordinates}?overview=full&geometries=geojson`,
    );

    if (!response.ok) {
      const fallback = toFallbackResult(safeFallback);
      routeCache.set(cacheKey, fallback);
      return fallback;
    }

    const payload = await response.json();
    const route = payload?.routes?.[0];
    const geometry = route?.geometry?.coordinates as [number, number][] | undefined;

    if (!geometry || geometry.length < 2) {
      const fallback = toFallbackResult(safeFallback);
      routeCache.set(cacheKey, fallback);
      return fallback;
    }

    const resolved: RoutingResult = {
      path: geometry.map(([lng, lat]) => ({ lat, lng })),
      distanceMeters:
        typeof route.distance === "number" ? route.distance : calculatePathDistanceMeters(safeFallback),
      durationSeconds:
        typeof route.duration === "number"
          ? Math.round(route.duration)
          : estimateDurationSeconds(calculatePathDistanceMeters(safeFallback)),
      source: "osrm",
    };

    routeCache.set(cacheKey, resolved);
    return resolved;
  } catch {
    const fallback = toFallbackResult(safeFallback);
    routeCache.set(cacheKey, fallback);
    return fallback;
  }
}

