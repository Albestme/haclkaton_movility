# Operary EV - Base de gestion de tecnicos

Base inicial para una empresa que instala y mantiene cargadores de vehiculos electricos.

Incluye:
- Panel de control con KPIs operativos
- Lista filtrable de ordenes de trabajo
- Detalle de orden seleccionada
- Sugerencia y asignacion de tecnico
- Gestion rapida de estado del equipo tecnico

## Estructura principal

- `app/page.tsx`: entrada de la aplicacion
- `src/components/operations-dashboard.tsx`: interfaz principal
- `src/features/operations/types.ts`: tipos de dominio
- `src/features/operations/data.ts`: datos semilla para MVP
- `src/features/operations/metrics.ts`: logica de filtrado y metricas
- `scripts/smoke.ts`: smoke test de logica base

## Comandos

```bash
npm run dev
npm run lint
npm run smoke
npm run check
npm run build
```

## Roadmap sugerido

1. Persistir datos con API (`app/api/*`) y base de datos.
2. Autenticacion por rol (coordinador, tecnico, supervisor).
3. Agenda con mapa, ventanas horarias y SLA.
4. Historial de repuestos, tiempos y costes por OT.
