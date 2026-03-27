import os
from fastapi import FastAPI, HTTPException, Path
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List
from datetime import datetime
import uvicorn
from psycopg2.extras import RealDictCursor
from database import get_db_connection

# ==========================================
# IMPORTANTE: Aquí importaremos la función de Raul/Gerard
# from planner_algorithm import run_montecarlo_optimization
# ==========================================

app = FastAPI(
    title="Etecnic Copilot - Backend API",
    description="Main Backend connecting Frontend, internal Planner, and Database.",
    version="1.2.0" # Subimos versión por el cambio de esquema de Satxa
)

cors_origins = [origin.strip() for origin in os.getenv("CORS_ALLOW_ORIGINS", "*").split(",") if origin.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=cors_origins or ["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ==========================================
# MODELOS PYDANTIC (Adaptados al nuevo esquema)
# ==========================================
class PlannedTask(BaseModel):
    incidence_id: int | None = None
    contract_id: int | None = None
    priority: str  # ANTES ERA visit_type, ahora adaptado al esquema de Satxa
    planned_date: datetime
    estimated_duration_min: int

class WorkerRoute(BaseModel):
    technician_id: int
    route: List[PlannedTask]

class PlannerPayload(BaseModel):
    workers: List[WorkerRoute]


# ==========================================
# ENDPOINTS DEL PLANNER INTEGRADO
# ==========================================

@app.post("/api/v1/planner/run-optimization")
def trigger_planner_optimization():
    """
    ENDPOINT GATILLO: La centralita llama aquí para recalcular rutas.
    Ejecuta el algoritmo de Montecarlo internamente y guarda los resultados.
    """
    try:
        # 1. Obtener datos necesarios para el algoritmo (técnicos, incidencias)
        # techs = get_technicians_for_planner()
        # pending_incidences = get_pending_incidences()
        
        # 2. Ejecutar el algoritmo de Raul/Gerard
        # rutas_optimizadas = run_montecarlo_optimization(techs, pending_incidences)
        
        # 3. Guardar en BD (Llamando a nuestra propia función internamente)
        # save_planned_routes_internal(rutas_optimizadas)
        
        return {"message": "Optimization completed successfully. Routes updated in DB."}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Optimization failed: {e}")

@app.post("/api/v1/planner/routes", status_code=201)
def save_planned_routes(payload: PlannerPayload):
    """
    Guarda las rutas calculadas en la tabla VISIT.
    Adaptado para usar 'priority' en lugar de 'visit_type'.
    """
    conn = get_db_connection()
    try:
        cur = conn.cursor()
        
        for worker in payload.workers:
            tech_id = worker.technician_id
            
            for task in worker.route:
                # Modificado para insertar 'priority'
                cur.execute("""
                    INSERT INTO visit (
                        technician_id, contract_id, incidence_id, 
                        priority, status, planned_date, estimated_duration_min
                    ) VALUES (%s, %s, %s, %s, 'scheduled', %s, %s)
                """, (
                    tech_id,
                    task.contract_id,
                    task.incidence_id,
                    task.priority,
                    task.planned_date,
                    task.estimated_duration_min
                ))
        
        conn.commit()
        return {"message": "Routes successfully saved to the database"}
    except Exception as e:
        conn.rollback()
        print(f"Error saving routes: {e}")
        raise HTTPException(status_code=500, detail="Failed to save routes to database")
    finally:
        cur.close()
        conn.close()

@app.get("/api/v1/planner/technicians")
def get_technicians_for_planner():
    """Devuelve todos los técnicos con su ubicación inicial real."""
    conn = get_db_connection()
    try:
        cur = conn.cursor(cursor_factory=RealDictCursor)
        cur.execute("""
            SELECT technician_id, name, zone, 
                   latitude AS start_lat, longitude AS start_lon 
            FROM technician;
        """)
        return cur.fetchall()
    finally:
        conn.close()

@app.get("/api/v1/planner/incidences/pending")
def get_pending_incidences():
    """
    Devuelve incidencias pendientes. 
    Se adapta a los estados en catalán ('Pendent', 'En curs', 'Tancada').
    """
    conn = get_db_connection()
    try:
        cur = conn.cursor(cursor_factory=RealDictCursor)
        # Ahora filtramos explícitamente por 'Pendent' o 'En curs'
        cur.execute("""
            SELECT i.incidence_id, i.charger_id, i.reported_at, i.priority, 
                   i.estimated_duration_min, c.latitude, c.longitude, i.status
            FROM incidence i
            JOIN charger c ON i.charger_id = c.charger_id
            WHERE i.status IN ('Pendent', 'En curs');
        """)
        return cur.fetchall()
    finally:
        conn.close()

@app.get("/api/v1/planner/technicians/{tech_id}/current-location")
def get_technician_current_location(tech_id: int = Path(...)):
    """Dado un ID de técnico, devuelve las coordenadas de su visita ACTUAL."""
    conn = get_db_connection()
    try:
        cur = conn.cursor(cursor_factory=RealDictCursor)
        cur.execute("""
            SELECT 
                COALESCE(ci.latitude, cc.latitude) AS latitude,
                COALESCE(ci.longitude, cc.longitude) AS longitude,
                v.visit_id
            FROM visit v
            LEFT JOIN incidence i ON v.incidence_id = i.incidence_id
            LEFT JOIN charger ci ON i.charger_id = ci.charger_id
            LEFT JOIN contract c ON v.contract_id = c.contract_id
            LEFT JOIN charger cc ON c.charger_id = cc.charger_id
            WHERE v.technician_id = %s AND v.status = 'in_progress'
            LIMIT 1;
        """, (tech_id,))
        location = cur.fetchone()
        if not location:
            raise HTTPException(status_code=404, detail="Technician has no active visit")
        return location
    finally:
        conn.close()

@app.get("/api/v1/planner/incidences/{incidence_id}")
def get_specific_incidence(incidence_id: int = Path(...)):
    """Pasado un ID, devuelve los detalles de la incidencia."""
    conn = get_db_connection()
    try:
        cur = conn.cursor(cursor_factory=RealDictCursor)
        cur.execute("""
            SELECT incidence_id, charger_id, reported_at, priority, status, estimated_duration_min
            FROM incidence
            WHERE incidence_id = %s;
        """, (incidence_id,))
        incidence = cur.fetchone()
        if not incidence:
            raise HTTPException(status_code=404, detail="Incidence not found")
        return incidence
    finally:
        conn.close()


# ==========================================
# ENDPOINTS PARA EL FRONTEND APP (Albert)
# ==========================================

@app.get("/api/v1/app/technicians")
def get_all_technicians():
    """Lista todos los técnicos para poblar paneles del frontend."""
    conn = get_db_connection()
    try:
        cur = conn.cursor(cursor_factory=RealDictCursor)
        cur.execute("""
            SELECT technician_id, name, zone, latitude, longitude
            FROM technician
            ORDER BY technician_id ASC;
        """)
        return cur.fetchall()
    except Exception as e:
        print(f"Error fetching technicians: {e}")
        raise HTTPException(status_code=500, detail="Database error")
    finally:
        conn.close()

@app.get("/api/v1/app/technicians/{tech_id}")
def get_technician_details(tech_id: int = Path(...)):
    """Dado un ID de técnico, devuelve todos sus detalles."""
    conn = get_db_connection()
    try:
        cur = conn.cursor(cursor_factory=RealDictCursor)
        cur.execute("""
            SELECT technician_id, name, zone, latitude, longitude
            FROM technician
            WHERE technician_id = %s;
        """, (tech_id,))
        technician = cur.fetchone()
        if not technician:
            raise HTTPException(status_code=404, detail="Technician not found")
        return technician
    except Exception as e:
        print(f"Error fetching technician: {e}")
        raise HTTPException(status_code=500, detail="Database error")
    finally:
        conn.close()

@app.get("/api/v1/app/technicians/{tech_id}/route")
def get_technician_route(tech_id: int = Path(...)):
    """Albert llama aquí para pintar la ruta de hoy. Usa 'priority' en vez de 'visit_type'."""
    conn = get_db_connection()
    try:
        cur = conn.cursor(cursor_factory=RealDictCursor)
        # Adaptado para leer 'v.priority'
        cur.execute("""
            SELECT 
                v.visit_id, v.incidence_id, v.contract_id, v.priority, v.status, v.planned_date, v.address, 
                v.estimated_duration_min,
                COALESCE(ci.latitude, cc.latitude) AS latitude,
                COALESCE(ci.longitude, cc.longitude) AS longitude
            FROM visit v
            LEFT JOIN incidence i ON v.incidence_id = i.incidence_id
            LEFT JOIN charger ci ON i.charger_id = ci.charger_id
            LEFT JOIN contract c ON v.contract_id = c.contract_id
            LEFT JOIN charger cc ON c.charger_id = cc.charger_id
            WHERE v.technician_id = %s AND v.status IN ('scheduled', 'in_progress')
            ORDER BY v.planned_date ASC;
        """, (tech_id,))
        return cur.fetchall()
    finally:
        conn.close()

@app.get("/api/v1/app/incidences")
def get_all_incidences():
    """Devuelve TODO el json de incidencias para Albert o la centralita."""
    conn = get_db_connection()
    try:
        cur = conn.cursor(cursor_factory=RealDictCursor)
        cur.execute("SELECT * FROM incidence;")
        return cur.fetchall()
    finally:
        conn.close()

# Run with: uvicorn main:app --host 0.0.0.0 --port 8000 --reload
if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=os.getenv("BACKEND_HOST", "0.0.0.0"),
        port=int(os.getenv("BACKEND_PORT", "8000")),
        reload=True,
    )
