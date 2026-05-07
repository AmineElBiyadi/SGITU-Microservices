"""
Algorithm: Zone risk ranking.
Computes a raw risk score using weighted incident counts (CRITICAL=4 to LOW=1).
Normalizes scores using min-max scaling (0-1) and maps them to HIGH, MEDIUM, or LOW risk levels.
"""

from fastapi import APIRouter
from datetime import datetime
import pandas as pd
import numpy as np
from models.schemas import IncidentsRequest, IncidentsResponse, ZoneRisk

router = APIRouter(prefix="/predict", tags=["predictions"])

SEVERITY_WEIGHTS = {
    "CRITICAL": 4,
    "HIGH": 3,
    "MEDIUM": 2,
    "LOW": 1
}

def get_risk_level(score: float) -> str:
    if score >= 0.7:
        return "HIGH"
    if score >= 0.4:
        return "MEDIUM"
    return "LOW"

@router.post("/incidents", response_model=IncidentsResponse)
async def predict_incidents(request: IncidentsRequest):
    if not request.data:
        return IncidentsResponse(
            at_risk_zones=[],
            generatedAt=datetime.now()
        )

    # Load data into DataFrame
    df = pd.DataFrame([dp.dict() for dp in request.data])
    
    # Calculate raw scores
    df['weight'] = df['severity'].map(lambda x: SEVERITY_WEIGHTS.get(x.upper(), 1))
    df['raw_score'] = df['incidentCount'] * df['weight']
    
    # Normalize raw scores
    min_score = df['raw_score'].min()
    max_score = df['raw_score'].max()
    
    if len(df) == 1:
        # Single zone case
        df['riskScore'] = 1.0
    elif max_score == min_score:
        # All zones have the same raw score
        df['riskScore'] = 0.5
    else:
        # Min-max normalization
        df['riskScore'] = (df['raw_score'] - min_score) / (max_score - min_score)
    
    # Ensure incidentCount 0 results in 0 riskScore if not already handled by normalization
    df.loc[df['incidentCount'] == 0, 'riskScore'] = 0.0
    
    # Assign risk levels
    df['riskLevel'] = df['riskScore'].apply(get_risk_level)
    
    # Sort by riskScore descending
    df_sorted = df.sort_values(by='riskScore', ascending=False)
    
    # Create response list
    at_risk_zones = [
        ZoneRisk(
            zone=row['zone'], 
            riskScore=float(row['riskScore']), 
            riskLevel=row['riskLevel']
        )
        for _, row in df_sorted.iterrows()
    ]

    return IncidentsResponse(
        at_risk_zones=at_risk_zones,
        generatedAt=datetime.now()
    )
