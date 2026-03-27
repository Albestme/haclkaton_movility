# Operary EV - Base de gestion de tecnicos

Base inicial para una empresa que instala y mantiene cargadores de vehiculos electricos.

Incluye:
- Menu lateral estilo app para moverte entre pestañas
- Panel de control con KPIs operativos
- Panel de control temporal para entradas de tareas por intervalo
- Pantalla de rutas con OpenStreetMap para tecnicos asignados
- Panel de tareas con todas las OT del sistema
- Lista filtrable de ordenes de trabajo
- Detalle de orden seleccionada
- Sugerencia y asignacion de tecnico
- Gestion rapida de estado del equipo tecnico

## Estructura principal

- `app/page.tsx`: redirige a `/panel`
- `app/(dashboard)/layout.tsx`: shell con menu lateral
- `app/(dashboard)/panel/page.tsx`: panel operativo principal
- `app/(dashboard)/control/page.tsx`: panel de entradas por tiempo
- `app/(dashboard)/rutas/page.tsx`: mapa de rutas de tecnicos
- `app/(dashboard)/tareas/page.tsx`: listado completo de tareas
- `app/(dashboard)/tecnicos/page.tsx`: vista de tecnicos
- `src/components/sidebar-nav.tsx`: menu lateral
- `src/components/operations-dashboard.tsx`: interfaz operativa principal
- `src/features/operations/types.ts`: tipos de dominio
- `src/features/operations/data.ts`: datos semilla para fallback
- `src/features/operations/backend.ts`: cliente server-side para backend_metrics
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

## Variables de entorno

- `BACKEND_API_URL`: URL base del backend FastAPI (`backend_metrics`).
  - Valor por defecto: `http://127.0.0.1:8000`
  - Ejemplo local: `BACKEND_API_URL=http://127.0.0.1:8000`
- `NEXT_PUBLIC_OSRM_BASE_URL`: endpoint base para calcular rutas de carretera en la pantalla `rutas`.
  - Valor por defecto: `https://router.project-osrm.org`
  - Ejemplo local: `NEXT_PUBLIC_OSRM_BASE_URL=https://router.project-osrm.org`

## Integracion con backend_metrics

1. Copia `web/.env.example` a `web/.env.local`.
2. Copia `backend_metrics/.env.example` a `backend_metrics/.env` y completa `DB_USER`/`DB_PASSWORD`.
3. Arranca backend y frontend en terminales separadas.

```bash
# Terminal 1
cd backend_metrics
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload

# Terminal 2
cd web
npm install
npm run dev
```

## Roadmap sugerido

1. Persistir datos con API (`app/api/*`) y base de datos.
2. Autenticacion por rol (coordinador, tecnico, supervisor).
3. Agenda con mapa, ventanas horarias y SLA.
4. Historial de repuestos, tiempos y costes por OT.
