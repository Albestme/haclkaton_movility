# backend_metrics

Backend FastAPI para operaciones tecnicas y planner.

## Setup rapido

```bash
pip install -r requirements.txt
cp .env.example .env
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

## Variables de entorno

- `DB_USER`: usuario de PostgreSQL
- `DB_PASSWORD`: password de PostgreSQL
- `BACKEND_HOST`: host de arranque (default `0.0.0.0`)
- `BACKEND_PORT`: puerto de arranque (default `8000`)
- `CORS_ALLOW_ORIGINS`: lista separada por comas de origins permitidos

## Endpoints para web

- `GET /api/v1/app/technicians`
- `GET /api/v1/app/technicians/{tech_id}`
- `GET /api/v1/app/technicians/{tech_id}/route`
- `GET /api/v1/app/incidences`

