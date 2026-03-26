from pydantic import BaseModel
from typing import List, Optional

class DashboardMetrics(BaseModel):
    total_completed: int
    total_pending: int
    completion_ratio: float
    avg_delay_minutes: float

class TechnicianWellBeing(BaseModel):
    technician_id: int
    name: str
    effective_working_hours: float
    kilometers_traveled: float
    travel_costs: float
    well_being_index: float  # e.g., a score out of 100
    grace_period_violations: int

class MetricsResponse(BaseModel):
    dashboard_overview: DashboardMetrics
    technicians_ranking: List[TechnicianWellBeing]