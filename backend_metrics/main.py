from fastapi import FastAPI, HTTPException
import uvicorn
from models import DashboardMetrics, TechnicianWellBeing, MetricsResponse

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
    Returns global observability metrics for the operations center.
    """
    # TODO: Replace this mock data with actual SQL queries using psycopg2
    
    # Mocking Dashboard Metrics
    overview = DashboardMetrics(
        total_completed=85,
        total_pending=15,
        completion_ratio=85.0,
        avg_delay_minutes=12.5
    )
    
    # Mocking Technician Metrics
    tech_1 = TechnicianWellBeing(
        technician_id=1,
        name="Marc Solà",
        effective_working_hours=32.5,
        kilometers_traveled=120.4,
        travel_costs=45.15,
        well_being_index=88.0,
        grace_period_violations=0
    )
    
    tech_2 = TechnicianWellBeing(
        technician_id=2,
        name="Laura Ferrer",
        effective_working_hours=38.0,
        kilometers_traveled=190.2,
        travel_costs=71.32,
        well_being_index=65.5, # Lower score due to more km/hours
        grace_period_violations=2
    )
    
    return MetricsResponse(
        dashboard_overview=overview,
        technicians_ranking=[tech_1, tech_2]
    )

# Run with: uvicorn main:app --reload
if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)