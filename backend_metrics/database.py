from fastapi import FastAPI, HTTPException
import uvicorn
from psycopg2.extras import RealDictCursor
from models import DashboardMetrics, TechnicianWellBeing, MetricsResponse
from database import get_db_connection

app = FastAPI(
    title="Etecnic Copilot - Metrics API",
    description="Observability and metrics for the operations dashboard.",
    version="1.0.0"
)

@app.get("/")
def read_root():
    return {"status": "Metrics API is running!"}

@app.get("/api/v1/metrics/dashboard", response_model=MetricsResponse)
def get_global_metrics():
    """
    Returns global observability metrics for the operations center using real DB data.
    """
    conn = None
    try:
        conn = get_db_connection()
        # RealDictCursor allows us to access columns by name (e.g., row['total_completed'])
        cur = conn.cursor(cursor_factory=RealDictCursor)
        
        # 1. Query for Visits (Completed vs Pending)
        cur.execute("""
            SELECT 
                COUNT(*) FILTER (WHERE status = 'completed') as total_completed,
                COUNT(*) FILTER (WHERE status IN ('scheduled', 'in_progress')) as total_pending
            FROM visit;
        """)
        visits_data = cur.fetchone()
        
        total_comp = visits_data['total_completed'] or 0
        total_pend = visits_data['total_pending'] or 0
        total_visits = total_comp + total_pend
        
        # Calculate ratio safely avoiding division by zero
        ratio = (total_comp / total_visits * 100) if total_visits > 0 else 0.0

        # 2. Query for Average Delay (from incidences)
        # Delay = final_duration - estimated_duration
        cur.execute("""
            SELECT AVG(final_duration_min - estimated_duration_min) as avg_delay
            FROM incidence
            WHERE status = 'resolved' 
              AND final_duration_min IS NOT NULL 
              AND estimated_duration_min IS NOT NULL;
        """)
        delay_data = cur.fetchone()
        avg_delay = delay_data['avg_delay'] if delay_data['avg_delay'] is not None else 0.0

        # Construct the DashboardMetrics object with REAL data
        overview = DashboardMetrics(
            total_completed=total_comp,
            total_pending=total_pend,
            completion_ratio=round(ratio, 2),
            avg_delay_minutes=round(avg_delay, 2)
        )
        
        # TODO: Replace this mock data with actual SQL queries for technicians
        tech_1 = TechnicianWellBeing(
            technician_id=1,
            name="Marc Solà",
            effective_working_hours=32.5,
            kilometers_traveled=120.4,
            travel_costs=45.15,
            well_being_index=88.0,
            grace_period_violations=0
        )
        
        cur.close()
        
        return MetricsResponse(
            dashboard_overview=overview,
            technicians_ranking=[tech_1] # Only returning one mock tech for now
        )

    except Exception as e:
        print(f"Database error: {e}")
        raise HTTPException(status_code=500, detail="Error fetching metrics from database")
    finally:
        if conn:
            conn.close()

# Run with: uvicorn main:app --reload
if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)