from pydantic import BaseModel
from typing import List, Dict, Any
from datetime import datetime

# --- Peak Hours Schemas ---

class PeakHourDataPoint(BaseModel):
    hour: int
    validationCount: int

class PeakHoursRequest(BaseModel):
    data: List[PeakHourDataPoint]

class HourScore(BaseModel):
    hour: int
    score: float

class PeakHoursResponse(BaseModel):
    predicted_peak_hours: List[int]
    distribution: List[HourScore]
    generatedAt: datetime

# --- Incidents Schemas ---

class IncidentDataPoint(BaseModel):
    zone: str
    incidentCount: int
    severity: str

class IncidentsRequest(BaseModel):
    data: List[IncidentDataPoint]

class ZoneRisk(BaseModel):
    zone: str
    riskScore: float
    riskLevel: str

class IncidentsResponse(BaseModel):
    at_risk_zones: List[ZoneRisk]
    generatedAt: datetime
